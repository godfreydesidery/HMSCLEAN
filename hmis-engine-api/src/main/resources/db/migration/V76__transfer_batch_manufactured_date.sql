-- V76: Propagate manufactured_date end-to-end on store → pharmacy transfers.
--
-- store_stock_batch already carries manufactured_date; the pharmacy side and
-- the per-transfer batch-pick snapshots dropped it, so a store→pharmacy
-- transfer preserved expiry but lost the manufactured date (legacy kept it).
-- Add the column to the pharmacy batch and to the three batch-pick tables so
-- the date can flow from source batch → pick snapshot → destination batch.

ALTER TABLE pharmacy_stock_batch ADD COLUMN manufactured_date date;
ALTER TABLE store_to_pharmacy_to_batch_pick ADD COLUMN manufactured_date date;
ALTER TABLE pharmacy_to_pharmacy_to_batch_pick ADD COLUMN manufactured_date date;
ALTER TABLE pharmacy_store_return_batch_pick ADD COLUMN manufactured_date date;
