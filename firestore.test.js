const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_uid_1";
const ADMIN_EMAIL = "ahmednagiatiyasaid@gmail.com";
const OWNER_UID = "owner_uid_1";
const VISITOR_UID = "visitor_uid_1";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function adminContext() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  });
}

function spoofedAdminContext() {
  return testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  });
}

function makeStorePayload(ownerId, past, active = true) {
  return {
    ownerId,
    ownerName: "أحمد محمد",
    name: "متجر النخيل للتمور",
    description: "أجود أنواع التمور الفاخرة والهدايا العربية.",
    address: "شارع الملك فهد، حي العليا",
    city: "الرياض",
    category: "مطاعم",
    logoUrl: "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=400",
    whatsappPhone: "966501234567",
    active,
    featured: true,
    spotlightFee: 150,
    ratingAvg: 4.8,
    ratingCount: 12,
    viewsCount: 100,
    productsCount: 3,
    planName: "تجريبية 30 يوم",
    subscriptionStatus: "active",
    subscriptionEndMillis: Date.now() + 86400000 * 30,
    createdAt: past,
    updatedAt: past,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write any marketplace collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("stores").where("active", "==", true).get());
  await assertFails(unauthDb.collection("products").where("active", "==", true).get());
  await assertFails(unauthDb.collection("joinRequests").get());
  await assertFails(
    unauthDb.collection("stores").doc("store_1").set(makeStorePayload(OWNER_UID, new Date(Date.now() - 1000)))
  );
});

test("Spoofed unverified admin email is rejected on admin actions", async () => {
  const spoofDb = spoofedAdminContext().firestore();
  const past = new Date(Date.now() - 2000);
  await assertFails(spoofDb.collection("stores").doc("store_1").set(makeStorePayload(OWNER_UID, past)));
  await assertFails(
    spoofDb.collection("categories").doc("cat_1").set({
      name: "مطاعم",
      sortOrder: 1,
      active: true,
      createdAt: past,
    })
  );
});

test("Admin can create stores, subscriptions, categories; non-admin cannot create stores or subscriptions", async () => {
  const adminDb = adminContext().firestore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertFails(ownerDb.collection("stores").doc("store_1").set(makeStorePayload(OWNER_UID, past)));
  await assertSucceeds(adminDb.collection("stores").doc("store_1").set(makeStorePayload(OWNER_UID, past)));

  await assertFails(
    ownerDb.collection("subscriptions").doc("sub_1").set({
      storeId: "store_1",
      storeName: "متجر النخيل للتمور",
      ownerId: OWNER_UID,
      plan: "trial_30",
      price: 0,
      status: "active",
      startDateMillis: Date.now() - 1000,
      endDateMillis: Date.now() + 86400000 * 30,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    adminDb.collection("subscriptions").doc("sub_1").set({
      storeId: "store_1",
      storeName: "متجر النخيل للتمور",
      ownerId: OWNER_UID,
      plan: "trial_30",
      price: 0,
      status: "active",
      startDateMillis: Date.now() - 1000,
      endDateMillis: Date.now() + 86400000 * 30,
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("Store owner can update allowed store info, add/update products and coupons, but cannot escalate featured/active", async () => {
  const past = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("stores").doc("store_1").set(makeStorePayload(OWNER_UID, past));
  });

  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const visitorDb = testEnv.authenticatedContext(VISITOR_UID).firestore();

  // Owner updates allowed info
  await assertSucceeds(
    ownerDb.collection("stores").doc("store_1").update({
      name: "متجر النخيل المطور",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Shadow update / privilege escalation rejected for owner
  await assertFails(
    ownerDb.collection("stores").doc("store_1").update({
      featured: true,
      active: true,
      spotlightFee: 0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Owner creates product for own store
  await assertSucceeds(
    ownerDb.collection("products").doc("prod_1").set({
      storeId: "store_1",
      ownerId: OWNER_UID,
      name: "تمر مجدول فاخر",
      description: "كيلو تمر مجدول طازج",
      price: 65.0,
      imageUrl: "https://images.unsplash.com/photo-1596591606975-97ee5cef3a1e?w=400",
      active: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Visitor cannot create product in owner's store
  await assertFails(
    visitorDb.collection("products").doc("prod_hack").set({
      storeId: "store_1",
      ownerId: VISITOR_UID,
      name: "منتج مزيف",
      description: "اختراق",
      price: 10.0,
      imageUrl: "",
      active: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Owner creates coupon for own store
  await assertSucceeds(
    ownerDb.collection("coupons").doc("coup_1").set({
      storeId: "store_1",
      ownerId: OWNER_UID,
      code: "SAVE15",
      discountPercent: 15,
      expiryMillis: Date.now() + 86400000 * 10,
      active: true,
      createdAt: past,
    })
  );
});

test("Visitor can increment viewsCount by 1, place order, and submit review; order PII is isolated", async () => {
  const past = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("stores").doc("store_1").set(makeStorePayload(OWNER_UID, past));
  });

  const visitorDb = testEnv.authenticatedContext(VISITOR_UID).firestore();
  const otherDb = testEnv.authenticatedContext("other_user").firestore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();

  // Increment viewsCount by +1 succeeds; +5 fails
  await assertSucceeds(
    visitorDb.collection("stores").doc("store_1").update({
      viewsCount: 101,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    visitorDb.collection("stores").doc("store_1").update({
      viewsCount: 500,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Visitor places order
  await assertSucceeds(
    visitorDb.collection("orders").doc("ord_1").set({
      storeId: "store_1",
      storeName: "متجر النخيل للتمور",
      storeOwnerId: OWNER_UID,
      customerId: VISITOR_UID,
      customerName: "سارة خالد",
      customerPhone: "0551234567",
      customerAddress: "حي الملقا، الرياض",
      productId: "prod_1",
      productName: "تمر مجدول فاخر",
      unitPrice: 65.0,
      quantity: 2,
      discountPercent: 0,
      totalPrice: 130.0,
      status: "sent_whatsapp",
      createdAt: past,
    })
  );

  // Order PII cannot be read by another random visitor
  await assertFails(otherDb.collection("orders").doc("ord_1").get());
  // Owner and customer can read order
  await assertSucceeds(ownerDb.collection("orders").doc("ord_1").get());
  await assertSucceeds(visitorDb.collection("orders").doc("ord_1").get());

  // Visitor writes review
  await assertSucceeds(
    visitorDb.collection("reviews").doc("rev_1").set({
      storeId: "store_1",
      userId: VISITOR_UID,
      userName: "سارة خالد",
      rating: 5,
      comment: "خدمة ممتازة وجودة رائعة",
      createdAt: past,
    })
  );
});

test("JoinRequests: owner submits pending request; owner cannot self-approve; admin approves", async () => {
  const past = new Date(Date.now() - 5000);
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const otherDb = testEnv.authenticatedContext(VISITOR_UID).firestore();
  const adminDb = adminContext().firestore();

  await assertSucceeds(
    ownerDb.collection("joinRequests").doc("req_1").set({
      ownerId: OWNER_UID,
      ownerEmail: "owner@example.com",
      storeName: "بيت القهوة المختصة",
      ownerName: "أحمد",
      phone: "966509998877",
      city: "جدة",
      category: "مطاعم",
      address: "حي الروضة",
      description: "أفضل قهوة مختصة",
      logoUrl: "",
      status: "pending",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Owner cannot self-approve
  await assertFails(
    ownerDb.collection("joinRequests").doc("req_1").update({
      status: "approved",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Other user cannot read owner's join request
  await assertFails(otherDb.collection("joinRequests").doc("req_1").get());

  // Admin approves
  await assertSucceeds(
    adminDb.collection("joinRequests").doc("req_1").update({
      status: "approved",
      approvedStoreId: "store_new_1",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Query alignment tests: scoped queries succeed, unfiltered private queries fail", async () => {
  const past = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("stores").doc("store_active").set(makeStorePayload(OWNER_UID, past, true));
    await ctx.firestore().collection("stores").doc("store_hidden").set(makeStorePayload(OWNER_UID, past, false));
  });

  const visitorDb = testEnv.authenticatedContext(VISITOR_UID).firestore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const adminDb = adminContext().firestore();

  await assertSucceeds(visitorDb.collection("stores").where("active", "==", true).get());
  await assertFails(visitorDb.collection("stores").get());
  await assertSucceeds(ownerDb.collection("stores").where("ownerId", "==", OWNER_UID).get());
  await assertSucceeds(adminDb.collection("stores").get());
  await assertSucceeds(visitorDb.collection("orders").where("customerId", "==", VISITOR_UID).get());
  await assertFails(visitorDb.collection("orders").get());
});

test("User favorites: user can save, list, and remove own favorite stores; cross-user access is denied", async () => {
  const past = new Date(Date.now() - 5000);
  const visitorDb = testEnv.authenticatedContext(VISITOR_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  const favPayload = {
    userId: VISITOR_UID,
    storeId: "store_1",
    storeName: "متجر النخيل للتمور",
    storeCategory: "مطاعم",
    storeCity: "الرياض",
    storeLogoUrl: "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=400",
    createdAt: past,
  };

  await assertFails(
    unauthDb.collection("users").doc(VISITOR_UID).collection("favorites").doc("store_1").set(favPayload)
  );

  // User saves own favorite store
  await assertSucceeds(
    visitorDb.collection("users").doc(VISITOR_UID).collection("favorites").doc("store_1").set(favPayload)
  );
  await assertSucceeds(
    visitorDb.collection("users").doc(VISITOR_UID).collection("favorites").get()
  );

  // Another user cannot read, create, or delete visitor's favorites
  await assertFails(
    otherDb.collection("users").doc(VISITOR_UID).collection("favorites").get()
  );
  await assertFails(
    otherDb.collection("users").doc(VISITOR_UID).collection("favorites").doc("store_1").delete()
  );
  await assertFails(
    otherDb.collection("users").doc(VISITOR_UID).collection("favorites").doc("store_2").set({
      ...favPayload,
      storeId: "store_2",
      userId: OWNER_UID,
    })
  );

  // User deletes own favorite store
  await assertSucceeds(
    visitorDb.collection("users").doc(VISITOR_UID).collection("favorites").doc("store_1").delete()
  );
});
