import { createHash } from 'crypto'

/** 派生 SQLCipher 口令（用户库文件加密用） */
export function deriveKey(userId: string, dbSecret: string, purpose = ''): Buffer {
  return createHash('sha256')
    .update(userId + ':' + dbSecret + ':' + purpose)
    .digest()
}
