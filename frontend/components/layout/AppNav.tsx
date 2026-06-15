import Link from "next/link";

const LINKS = [
  { href: "/", label: "Simulator" },
  { href: "/wallet", label: "Wallet" },
  { href: "/audit", label: "Audit" },
  { href: "/login", label: "Login" },
  { href: "/profile", label: "Profile" },
];

export function AppNav() {
  return (
    <nav className="border-b border-slate-800 bg-surface px-4 py-3">
      <div className="mx-auto flex max-w-6xl items-center justify-between gap-4">
        <Link href="/" className="text-lg font-semibold text-slate-100">
          ETH Stablecoin Simulator
        </Link>
        <ul className="flex flex-wrap gap-3 text-sm">
          {LINKS.map((link) => (
            <li key={link.href}>
              <Link href={link.href} className="text-slate-300 hover:text-white">
                {link.label}
              </Link>
            </li>
          ))}
        </ul>
      </div>
    </nav>
  );
}