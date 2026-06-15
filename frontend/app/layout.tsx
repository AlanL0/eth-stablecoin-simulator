import { AppNav } from "@/components/layout/AppNav";
import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "ETH Stablecoin Simulator",
  description: "Educational ETH-backed stablecoin simulation with charts and treasury context",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        <AppNav />
        <main className="mx-auto max-w-6xl px-4 py-6">{children}</main>
      </body>
    </html>
  );
}