// Script to verify SQL migration syntax, completeness, and referential integrity
import fs from 'fs';
import path from 'path';

const migrationsDir = path.resolve('supabase/migrations');
const schemaFile = path.resolve('supabase/schema.sql');

console.log('=== Verifying AMAN SQL Migrations ===');

if (!fs.existsSync(migrationsDir)) {
    console.error('ERROR: migrations directory missing!');
    process.exit(1);
}

const files = fs.readdirSync(migrationsDir).filter(f => f.endsWith('.sql')).sort();
console.log(`Found ${files.length} migration files:`);
files.forEach(f => console.log(` - ${f}`));

let totalLines = 0;
const tablesDeclared = new Set();
const foreignKeyTargets = [];
const functionsDeclared = new Set();

files.forEach(file => {
    const content = fs.readFileSync(path.join(migrationsDir, file), 'utf8');
    totalLines += content.split('\n').length;

    // Check parenthetical balance
    let openParen = 0;
    for (let char of content) {
        if (char === '(') openParen++;
        if (char === ')') openParen--;
    }
    if (openParen !== 0) {
        console.error(`ERROR: Parentheses unbalanced in ${file} (diff: ${openParen})`);
        process.exit(1);
    }

    // Extract declared tables
    const tableMatches = content.matchAll(/CREATE TABLE (?:IF NOT EXISTS )?([a-z_]+)/gi);
    for (const match of tableMatches) {
        tablesDeclared.add(match[1].toLowerCase());
    }

    // Extract declared functions
    const funcMatches = content.matchAll(/CREATE OR REPLACE FUNCTION ([a-z_]+)/gi);
    for (const match of funcMatches) {
        functionsDeclared.add(match[1].toLowerCase());
    }

    // Extract foreign keys
    const fkMatches = content.matchAll(/REFERENCES ([a-z_\.]+)\s*\(([a-z_]+)\)/gi);
    for (const match of fkMatches) {
        foreignKeyTargets.push({ fromFile: file, targetTable: match[1].toLowerCase(), targetColumn: match[2].toLowerCase() });
    }
});

console.log('\n--- Declared Tables ---');
tablesDeclared.forEach(t => console.log(` ✓ Table: ${t}`));

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

for (const exp of expectedTables) {
    if (!tablesDeclared.has(exp)) {
        console.error(`ERROR: Missing expected table ${exp}`);
        process.exit(1);
    }
}

console.log('\n--- Declared RPC Functions ---');
functionsDeclared.forEach(f => console.log(` ✓ Function: ${f}`));

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

for (const exp of expectedFunctions) {
    if (!functionsDeclared.has(exp)) {
        console.error(`ERROR: Missing expected RPC function ${exp}`);
        process.exit(1);
    }
}

console.log('\n--- Validating Foreign Key Relationships ---');
for (const fk of foreignKeyTargets) {
    if (fk.targetTable.startsWith('auth.')) {
        console.log(` ✓ External Auth Target: ${fk.targetTable}(${fk.targetColumn})`);
    } else if (tablesDeclared.has(fk.targetTable)) {
        console.log(` ✓ Internal FK Valid: -> ${fk.targetTable}(${fk.targetColumn})`);
    } else {
        console.error(`ERROR: Foreign key target '${fk.targetTable}' in ${fk.fromFile} not found in declared tables!`);
        process.exit(1);
    }
}

// Also verify supabase/schema.sql exists and is non-empty
if (!fs.existsSync(schemaFile) || fs.statSync(schemaFile).size < 1000) {
    console.error('ERROR: supabase/schema.sql is missing or incomplete!');
    process.exit(1);
}

console.log(`\nAll ${files.length} SQL migrations verified successfully! (${totalLines} total lines)`);
