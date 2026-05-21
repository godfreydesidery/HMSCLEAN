/** A store keeper affiliated with a store (md_store_staff). */
export interface StoreStaff {
  uid: string;
  storeUid: string;
  userUid: string;
  username: string;
  fullName: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}
