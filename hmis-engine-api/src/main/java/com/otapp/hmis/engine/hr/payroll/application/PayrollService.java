package com.otapp.hmis.engine.hr.payroll.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.hr.employee.domain.Employee;
import com.otapp.hmis.engine.hr.employee.domain.EmployeeRepository;
import com.otapp.hmis.engine.hr.employee.domain.EmploymentStatus;
import com.otapp.hmis.engine.hr.payroll.application.PayrollDtos.CancelPayrollPeriodRequest;
import com.otapp.hmis.engine.hr.payroll.application.PayrollDtos.CreatePayrollPeriodRequest;
import com.otapp.hmis.engine.hr.payroll.application.PayrollDtos.ImportEmployeesResultDto;
import com.otapp.hmis.engine.hr.payroll.application.PayrollDtos.PayrollItemDto;
import com.otapp.hmis.engine.hr.payroll.application.PayrollDtos.PayrollItemLineDto;
import com.otapp.hmis.engine.hr.payroll.application.PayrollDtos.PayrollPeriodDto;
import com.otapp.hmis.engine.hr.payroll.application.PayrollDtos.PayrollPeriodWithItemsDto;
import com.otapp.hmis.engine.hr.payroll.application.PayrollDtos.UpsertPayrollItemRequest;
import com.otapp.hmis.engine.hr.payroll.domain.PayrollItem;
import com.otapp.hmis.engine.hr.payroll.domain.PayrollItemLine;
import com.otapp.hmis.engine.hr.payroll.domain.PayrollItemLineRepository;
import com.otapp.hmis.engine.hr.payroll.domain.PayrollItemRepository;
import com.otapp.hmis.engine.hr.payroll.domain.PayrollPeriod;
import com.otapp.hmis.engine.hr.payroll.domain.PayrollPeriodRepository;
import com.otapp.hmis.engine.hr.payroll.domain.PayrollPeriodStatus;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PayrollService {

    private final PayrollPeriodRepository periodRepository;
    private final PayrollItemRepository itemRepository;
    private final PayrollItemLineRepository itemLineRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    public PayrollPeriodDto createPeriod(CreatePayrollPeriodRequest request) {
        if (periodRepository.existsByCode(request.code().trim())) {
            throw new BusinessRuleException("Payroll period code already in use: " + request.code());
        }
        PayrollPeriod period = new PayrollPeriod(
                request.code(), request.label(),
                request.startDate(), request.endDate(),
                request.currency());
        period.setNote(emptyToNull(request.note()));
        periodRepository.save(period);
        return toDto(period);
    }

    @Transactional
    public PayrollItemDto upsertItem(String periodUid, UpsertPayrollItemRequest request) {
        PayrollPeriod period = loadPeriodOrThrow(periodUid);
        if (!period.isMutable()) {
            throw new BusinessRuleException("Payroll period is " + period.getStatus() + " and locked");
        }
        Employee employee = employeeRepository.findByUid(request.employeeUid())
                .orElseThrow(() -> new NotFoundException("Employee not found: " + request.employeeUid()));

        PayrollItem item = itemRepository
                .findByPeriodUidAndEmployeeUid(period.getUid(), employee.getUid())
                .orElseGet(() -> itemRepository.save(new PayrollItem(
                        period.getUid(), employee.getUid(),
                        request.grossPay(), request.totalDeductions())));
        item.setGrossPay(request.grossPay());
        item.setTotalDeductions(request.totalDeductions());
        item.recomputeNet();
        item.setEmployerContributions(
                request.employerContributions() == null ? BigDecimal.ZERO : request.employerContributions());
        item.setPaymentMethod(emptyToNull(request.paymentMethod()));
        item.setPaymentReference(emptyToNull(request.paymentReference()));
        item.setNote(emptyToNull(request.note()));

        // Replace the itemised breakdown (the legacy PayrollDetail) when provided.
        if (request.lines() != null) {
            itemLineRepository.deleteByItemUid(item.getUid());
            int order = 0;
            for (var line : request.lines()) {
                itemLineRepository.save(new PayrollItemLine(
                        item.getUid(), emptyToNull(line.code()), line.name().trim(),
                        line.type(), line.amount(), order++));
            }
        }
        return toItemDto(item, employee);
    }

    /**
     * Bulk-seed a {@link PayrollItem} for every ACTIVE + payable employee not
     * yet on the period, prefilled from {@code Employee.basicSalary} (legacy
     * {@code PayrollServiceImpl.importEmployees}). Idempotent: a re-import adds
     * only newcomers. Faithful to legacy, throws when nothing new is imported.
     *
     * <p>Net = basicSalary (gross = basicSalary, deductions = 0); employer
     * contributions seed to 0 and are filled in later per line.
     */
    @Transactional
    public ImportEmployeesResultDto importEmployees(String periodUid) {
        PayrollPeriod period = loadPeriodOrThrow(periodUid);
        if (!period.isMutable()) {
            throw new BusinessRuleException(
                    "Payroll period is " + period.getStatus() + " and locked; only DRAFT periods can be imported into");
        }

        List<Employee> roster = employeeRepository
                .findAllByEmploymentStatusAndPayableTrue(EmploymentStatus.ACTIVE);
        // Dedupe in-memory by employeeUid (one query, no N+1, no constraint-violation 500).
        Set<String> existing = new HashSet<>(itemRepository.findEmployeeUidsByPeriodUid(period.getUid()));

        int imported = 0;
        for (Employee e : roster) {
            if (existing.contains(e.getUid())) {
                continue;
            }
            BigDecimal basic = e.getBasicSalary() == null
                    ? BigDecimal.ZERO.setScale(2)
                    : e.getBasicSalary();
            // gross = basic, deductions = 0 => net = basic (legacy prefill).
            itemRepository.save(new PayrollItem(period.getUid(), e.getUid(), basic, BigDecimal.ZERO.setScale(2)));
            existing.add(e.getUid());
            imported++;
        }
        if (imported == 0) {
            throw new BusinessRuleException("Nothing to import");
        }
        int total = roster.size();
        return new ImportEmployeesResultDto(imported, total - imported, total);
    }

    @Transactional
    public void removeItem(String periodUid, String employeeUid) {
        PayrollPeriod period = loadPeriodOrThrow(periodUid);
        if (!period.isMutable()) {
            throw new BusinessRuleException("Payroll period is " + period.getStatus() + " and locked");
        }
        itemRepository.findByPeriodUidAndEmployeeUid(period.getUid(), employeeUid)
                .ifPresent(item -> {
                    itemLineRepository.deleteByItemUid(item.getUid());
                    itemRepository.delete(item);
                });
    }

    @Transactional
    public PayrollPeriodDto verify(String periodUid) {
        PayrollPeriod period = loadPeriodOrThrow(periodUid);
        if (itemRepository.findAllByPeriodUidOrderByCreatedAtAsc(period.getUid()).isEmpty()) {
            throw new BusinessRuleException("Cannot verify an empty payroll period");
        }
        period.verify(currentUsername());
        return toDto(period);
    }

    @Transactional
    public PayrollPeriodDto approve(String periodUid) {
        PayrollPeriod period = loadPeriodOrThrow(periodUid);
        period.approve(currentUsername());
        return toDto(period);
    }

    @Transactional
    public PayrollPeriodDto markPaid(String periodUid) {
        PayrollPeriod period = loadPeriodOrThrow(periodUid);
        period.markPaid();
        return toDto(period);
    }

    @Transactional
    public PayrollPeriodDto cancel(String periodUid, CancelPayrollPeriodRequest request) {
        PayrollPeriod period = loadPeriodOrThrow(periodUid);
        period.cancel(emptyToNull(request == null ? null : request.reason()));
        return toDto(period);
    }

    @Transactional(readOnly = true)
    public PayrollPeriodWithItemsDto findByUid(String periodUid) {
        PayrollPeriod period = loadPeriodOrThrow(periodUid);
        List<PayrollItem> items = itemRepository.findAllByPeriodUidOrderByCreatedAtAsc(period.getUid());
        return new PayrollPeriodWithItemsDto(
                toDto(period),
                items.stream().map(this::toItemDto).toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<PayrollPeriodDto> search(PayrollPeriodStatus status, Pageable pageable) {
        return PageResponse.from(periodRepository.search(status, pageable).map(this::toDto));
    }

    // ----- helpers -----------------------------------------------------------

    private PayrollPeriod loadPeriodOrThrow(String uid) {
        return periodRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Payroll period not found: " + uid));
    }

    private PayrollPeriodDto toDto(PayrollPeriod p) {
        List<PayrollItem> items = itemRepository.findAllByPeriodUidOrderByCreatedAtAsc(p.getUid());
        BigDecimal totalNet = itemRepository.sumNetForPeriod(p.getUid());
        return new PayrollPeriodDto(
                p.getUid(),
                p.getCode(),
                p.getLabel(),
                p.getStartDate(),
                p.getEndDate(),
                p.getCurrency(),
                p.getStatus(),
                p.getNote(),
                p.getVerifiedAt(),
                p.getVerifiedByUsername(),
                p.getApprovedAt(),
                p.getApprovedByUsername(),
                p.getPaidAt(),
                p.getCancelledAt(),
                p.getCancelReason(),
                items.size(),
                totalNet == null ? BigDecimal.ZERO : totalNet,
                p.getCreatedAt(),
                p.getUpdatedAt());
    }

    private PayrollItemDto toItemDto(PayrollItem item) {
        Employee employee = employeeRepository.findByUid(item.getEmployeeUid()).orElse(null);
        return toItemDto(item, employee);
    }

    private PayrollItemDto toItemDto(PayrollItem item, Employee employee) {
        List<PayrollItemLineDto> lines = itemLineRepository.findAllByItemUidOrderBySortOrderAsc(item.getUid()).stream()
                .map(l -> new PayrollItemLineDto(l.getUid(), l.getCode(), l.getName(), l.getType(),
                        l.getAmount(), l.getSortOrder()))
                .toList();
        return new PayrollItemDto(
                item.getId(),
                item.getUid(),
                item.getPeriodUid(),
                item.getEmployeeUid(),
                employee == null ? null : employee.getEmployeeNo(),
                employee == null ? null : employee.fullName(),
                item.getGrossPay(),
                item.getTotalDeductions(),
                item.getNetPay(),
                item.getEmployerContributions(),
                item.getPaymentMethod(),
                item.getPaymentReference(),
                item.getNote(),
                lines,
                item.getCreatedAt(),
                item.getUpdatedAt());
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }
}
