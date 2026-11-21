import { db } from '../src/db.js';
import { generateActivationCode, hashSecret } from '../src/crypto-utils.js';

const code = generateActivationCode();
const hash = hashSecret(code);
const sql = db();
try {
  await sql.begin(async tx => {
    await tx`delete from sukho_private.activation_codes where used_at is null`;
    await tx`
      insert into sukho_private.activation_codes (code_hash, expires_at)
      values (${hash}, now() + interval '15 minutes')
    `;
  });
  console.log(code.match(/.{1,4}/g)?.join('-'));
  console.log('Berlaku 15 menit; hanya dapat dipakai satu kali.');
} finally {
  await sql.end();
}

