const EVM_ADDRESS = /^0x[0-9a-fA-F]{40}$/;

export function isValidEvmAddress(address: string): boolean {
  return EVM_ADDRESS.test(address.trim());
}

export function normalizeEvmAddress(address: string): string {
  return address.trim().toLowerCase();
}