import fs from 'node:fs';
import path from 'node:path';

console.log('====================================================');
console.log('AMAN | أمان - Stage 4 Administration Verification');
console.log('====================================================\n');

let totalChecks = 0;
let passedChecks = 0;
let failedChecks = 0;

function assert(condition, description) {
  totalChecks++;
  if (condition) {
    passedChecks++;
    console.log(`✅ [PASS] ${description}`);
  } else {
    failedChecks++;
    console.error(`❌ [FAIL] ${description}`);
  }
}

// 1. Verify all required files exist
const requiredFiles = [
  'app/src/main/kotlin/com/aman/app/data/repository/AdminRepository.kt',
  'app/src/main/kotlin/com/aman/app/ui/components/AmanAdminDrawer.kt',
  'app/src/main/kotlin/com/aman/app/ui/screens/admin/AdminDashboardViewModel.kt',
  'app/src/main/kotlin/com/aman/app/ui/screens/admin/AdminDashboardScreen.kt',
  'app/src/main/kotlin/com/aman/app/ui/screens/admin/AdminCustomersViewModel.kt',
  'app/src/main/kotlin/com/aman/app/ui/screens/admin/AdminCustomersScreen.kt',
  'app/src/main/kotlin/com/aman/app/ui/screens/admin/AdminCustomerDetailsScreen.kt',
  'app/src/main/kotlin/com/aman/app/ui/screens/admin/AdminProtectionRequestsViewModel.kt',
  'app/src/main/kotlin/com/aman/app/ui/screens/admin/AdminProtectionRequestsScreen.kt',
  'app/src/main/kotlin/com/aman/app/ui/screens/admin/AdminProtectionsViewModel.kt',
  'app/src/main/kotlin/com/aman/app/ui/screens/admin/AdminProtectionsScreen.kt',
  'app/src/main/kotlin/com/aman/app/ui/screens/admin/AdminTelecomProvidersViewModel.kt',
  'app/src/main/kotlin/com/aman/app/ui/screens/admin/AdminTelecomProvidersScreen.kt',
  'app/src/main/kotlin/com/aman/app/ui/screens/admin/AdminProtectionPlansViewModel.kt',
  'app/src/main/kotlin/com/aman/app/ui/screens/admin/AdminProtectionPlansScreen.kt',
  'app/src/main/kotlin/com/aman/app/ui/screens/admin/AdminPaymentMethodsViewModel.kt',
  'app/src/main/kotlin/com/aman/app/ui/screens/admin/AdminPaymentMethodsScreen.kt',
  'app/src/main/kotlin/com/aman/app/ui/screens/admin/AdminPaymentTasksViewModel.kt',
  'app/src/main/kotlin/com/aman/app/ui/screens/admin/AdminPaymentTasksScreen.kt',
  'app/src/main/kotlin/com/aman/app/ui/screens/admin/AdminTaskSettingsViewModel.kt',
  'app/src/main/kotlin/com/aman/app/ui/screens/admin/AdminTaskSettingsScreen.kt',
  'app/src/main/kotlin/com/aman/app/ui/screens/admin/AdminSystemSettingsViewModel.kt',
  'app/src/main/kotlin/com/aman/app/ui/screens/admin/AdminSystemSettingsScreen.kt',
  'app/src/main/kotlin/com/aman/app/ui/screens/admin/AdminAuditLogsViewModel.kt',
  'app/src/main/kotlin/com/aman/app/ui/screens/admin/AdminAuditLogsScreen.kt',
  'app/src/main/kotlin/com/aman/app/ui/navigation/Screen.kt',
  'app/src/main/kotlin/com/aman/app/ui/navigation/AmanNavGraph.kt'
];

for (const relPath of requiredFiles) {
  const exists = fs.existsSync(path.resolve(relPath));
  assert(exists, `Required file exists: ${relPath}`);
}

// 2. Verify AdminRepository contains all required operations and RPC calls
const adminRepoContent = fs.readFileSync('app/src/main/kotlin/com/aman/app/data/repository/AdminRepository.kt', 'utf-8');

const expectedMethods = [
  'loadDashboard',
  'getCustomers',
  'getCustomerDetails',
  'getProtectionRequests',
  'approveProtectionRequest',
  'rejectProtectionRequest',
  'getAllProtections',
  'getAllProviders',
  'createProvider',
  'disableProvider',
  'getAllPlans',
  'createPlan',
  'disablePlan',
  'getAllPaymentMethods',
  'createPaymentMethod',
  'disablePaymentMethod',
  'getAllTasks',
  'completePaymentTask',
  'cancelPaymentTask',
  'reschedulePaymentTask',
  'getTaskSettings',
  'updateTaskSettings',
  'getSystemSettings',
  'updateSystemSettings',
  'getAuditLogs'
];

for (const method of expectedMethods) {
  assert(adminRepoContent.includes(method), `AdminRepository defines: ${method}`);
}

// 3. Verify RPC calls in AdminRepository
const expectedRpcs = [
  'approve_protection_request',
  'reject_protection_request',
  'complete_payment_task',
  'cancel_payment_task',
  'reschedule_payment_task'
];

for (const rpc of expectedRpcs) {
  assert(adminRepoContent.includes(`"${rpc}"`), `AdminRepository calls secure RPC: ${rpc}`);
}

// 4. Verify no mock / fake data exists
const filesToCheckForMocks = requiredFiles.filter(f => f.endsWith('.kt'));
for (const relPath of filesToCheckForMocks) {
  const content = fs.readFileSync(path.resolve(relPath), 'utf-8');
  assert(!content.includes('listOf(mock') && !content.includes('FakeRepository') && !content.includes('MockData'),
    `No mock data in: ${relPath}`);
}

// 5. Verify bracket balance & basic syntax sanity
for (const relPath of filesToCheckForMocks) {
  const content = fs.readFileSync(path.resolve(relPath), 'utf-8');
  const openBraces = (content.match(/{/g) || []).length;
  const closeBraces = (content.match(/}/g) || []).length;
  const openParens = (content.match(/\(/g) || []).length;
  const closeParens = (content.match(/\)/g) || []).length;
  assert(openBraces === closeBraces, `Balanced braces in ${relPath} (${openBraces} / ${closeBraces})`);
  assert(openParens === closeParens, `Balanced parentheses in ${relPath} (${openParens} / ${closeParens})`);
}

// 6. Verify Navigation Graph has all routes mapped
const navGraphContent = fs.readFileSync('app/src/main/kotlin/com/aman/app/ui/navigation/AmanNavGraph.kt', 'utf-8');
const expectedRoutesInNavGraph = [
  'Screen.AdminDashboard.route',
  'Screen.AdminCustomers.route',
  'Screen.AdminCustomerDetails.route',
  'Screen.AdminProtectionRequests.route',
  'Screen.AdminProtections.route',
  'Screen.AdminTelecomProviders.route',
  'Screen.AdminProtectionPlans.route',
  'Screen.AdminPaymentMethods.route',
  'Screen.AdminPaymentTasks.route',
  'Screen.AdminTaskSettings.route',
  'Screen.AdminSystemSettings.route',
  'Screen.AdminAuditLogs.route'
];

for (const route of expectedRoutesInNavGraph) {
  assert(navGraphContent.includes(route), `AmanNavGraph routes to ${route}`);
}

// 7. Verify Admin Drawer has all menu items
const drawerContent = fs.readFileSync('app/src/main/kotlin/com/aman/app/ui/components/AmanAdminDrawer.kt', 'utf-8');
const expectedDrawerRoutes = [
  'Screen.AdminDashboard.route',
  'Screen.AdminCustomers.route',
  'Screen.AdminProtectionRequests.route',
  'Screen.AdminProtections.route',
  'Screen.AdminTelecomProviders.route',
  'Screen.AdminProtectionPlans.route',
  'Screen.AdminPaymentMethods.route',
  'Screen.AdminPaymentTasks.route',
  'Screen.AdminTaskSettings.route',
  'Screen.AdminSystemSettings.route',
  'Screen.AdminAuditLogs.route'
];

for (const r of expectedDrawerRoutes) {
  assert(drawerContent.includes(r), `AmanAdminDrawer links to ${r}`);
}

console.log('\n====================================================');
console.log(`Verification Complete: ${passedChecks}/${totalChecks} checks passed!`);
if (failedChecks > 0) {
  console.error(`Status: ${failedChecks} checks failed.`);
  process.exit(1);
} else {
  console.log('Status: ALL STAGE 4 CHECKS PASSED PERFECTLY!');
  console.log('====================================================');
}
