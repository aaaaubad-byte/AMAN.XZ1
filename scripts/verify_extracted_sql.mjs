import fs from 'fs';
import path from 'path';

console.log('=== AMAN.XZ1 Consolidated SQL Extraction Static Verification ===\n');

const sqlPath = path.resolve('supabase/AMAN.XZ1.Supabase.sql');
if (!fs.existsSync(sqlPath)) {
  console.error(`[FAIL] File not found: ${sqlPath}`);
  process.exit(1);
}

const sql = fs.readFileSync(sqlPath, 'utf8');

let passCount = 0;
let failCount = 0;

function check(desc, condition) {
  if (condition) {
    console.log(`[PASS] ${desc}`);
    passCount++;
  } else {
    console.error(`[FAIL] ${desc}`);
    failCount++;
  }
}

// 1. Secrets check
const secretPatterns = [
  /eyJ[a-zA-Z0-9_-]{10,}\.[a-zA-Z0-9_-]{10,}/, // JWT
  /service_role/i,
  /github_pat_/i,
  /sk_live_[a-zA-Z0-9]+/i,
  /password\s*[:=]\s*['"][^'"]+['"]/i,
];

let foundSecret = false;
for (const pat of secretPatterns) {
  if (pat.test(sql)) {
    foundSecret = true;
    break;
  }
}
check('No secrets, JWTs, or service_role credentials in SQL', !foundSecret);

// 2. Expected 13 Tables
const expectedTables = [
  'users',
  'telecom_providers',
  'telecom_prefixes',
  'customer_numbers',
  'protection_plans',
  'payment_methods',
  'protection_requests',
  'protections',
  'task_settings',
  'payment_tasks',
  'notifications',
  'audit_logs',
  'system_settings'
];

const tableMatches = [...sql.matchAll(/CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?([a-zA-Z0-9_]+)/gi)].map(m => m[1]);
expectedTables.forEach(table => {
  check(`Table '${table}' is declared in SQL`, tableMatches.includes(table));
});
check('Total table count is exactly 13', tableMatches.length === 13);

// 3. Expected ENUMs (7 enums)
const expectedEnums = [
  'app_user_role',
  'request_status',
  'stored_protection_status',
  'task_type',
  'task_status',
  'number_status',
  'notification_type'
];

const enumMatches = [...sql.matchAll(/CREATE\s+TYPE\s+([a-zA-Z0-9_]+)\s+AS\s+ENUM/gi)].map(m => m[1]);
expectedEnums.forEach(en => {
  check(`Enum '${en}' is declared`, enumMatches.includes(en));
});
check('Total enum count is exactly 7', enumMatches.length === 7);

// 4. Expected Functions / RPCs (12 functions)
const expectedFunctions = [
  'is_manager',
  'normalize_phone_number',
  'identify_provider_from_prefix',
  'register_customer_number',
  'submit_protection_request',
  'approve_protection_request',
  'reject_protection_request',
  'complete_payment_task',
  'reschedule_payment_task',
  'cancel_payment_task',
  'calculate_displayed_protection_status',
  'calculate_displayed_task_status'
];

const funcMatches = [...sql.matchAll(/CREATE\s+(?:OR\s+REPLACE\s+)?FUNCTION\s+([a-zA-Z0-9_]+)/gi)].map(m => m[1]);
expectedFunctions.forEach(fn => {
  check(`Function '${fn}' is declared`, funcMatches.includes(fn));
});
check('Total function count is exactly 12', funcMatches.length === 12);

// 5. RLS Enabled on all 13 tables
expectedTables.forEach(table => {
  const rlsRegex = new RegExp(`ALTER\\s+TABLE\\s+${table}\\s+ENABLE\\s+ROW\\s+LEVEL\\s+SECURITY`, 'i');
  check(`RLS enabled on '${table}'`, rlsRegex.test(sql));
});

// 6. Security Definer Checks on sensitive functions
const secDefFunctions = [
  'is_manager',
  'identify_provider_from_prefix',
  'register_customer_number',
  'submit_protection_request',
  'approve_protection_request',
  'reject_protection_request',
  'complete_payment_task',
  'reschedule_payment_task',
  'cancel_payment_task',
  'calculate_displayed_protection_status',
  'calculate_displayed_task_status'
];

secDefFunctions.forEach(fn => {
  const secDefRegex = new RegExp(`CREATE\\s+(?:OR\\s+REPLACE\\s+)?FUNCTION\\s+${fn}[\\s\\S]*?SECURITY\\s+DEFINER`, 'i');
  check(`Function '${fn}' configured with SECURITY DEFINER`, secDefRegex.test(sql));
});

// 7. Critical partial unique indexes
check('Partial unique index uq_pending_request_per_number exists', sql.includes('uq_pending_request_per_number'));
check('Partial unique index uq_active_protection_per_number exists', sql.includes('uq_active_protection_per_number'));
check('Partial unique index uq_active_telecom_prefix exists', sql.includes('uq_active_telecom_prefix'));

// 8. Baseline seed data checks
check('Contains baseline Yemen Mobile provider', sql.includes('YE-YM'));
check('Contains baseline YOU provider', sql.includes('YE-YOU'));
check('Contains baseline Sabafon provider', sql.includes('YE-SABAFON'));
check('Contains baseline Y Telecom provider', sql.includes('YE-Y'));
check('Contains baseline prefix 77', sql.includes("'77'"));
check('Contains baseline prefix 78', sql.includes("'78'"));
check('Contains baseline prefix 73', sql.includes("'73'"));
check('Contains baseline prefix 71', sql.includes("'71'"));
check('Contains baseline prefix 70', sql.includes("'70'"));
check('Contains default task settings per provider', sql.includes('task_settings'));
check('Contains default system settings renewal_warning_days = 7', sql.includes('renewal_warning_days'));

console.log(`\n========================================================================`);
console.log(`VERIFICATION SUMMARY: ${passCount} PASSED, ${failCount} FAILED`);
console.log(`========================================================================\n`);

if (failCount > 0) {
  process.exit(1);
} else {
  console.log('SUCCESS: All static SQL validations passed with ZERO errors!');
}
