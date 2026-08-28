"use client";

import Image from "next/image";
import Link from "next/link";
import { usePathname } from "next/navigation";

/**
 * 全画面共通ヘッダー。
 * - QA画面（ライブ画面 = パスが `/qa` で終わる）のみブランド青帯
 * - それ以外の画面は白ヘッダー
 */
export function AppHeader() {
  const pathname = usePathname();
  const onLive = pathname?.endsWith("/qa") ?? false;

  return (
    <header
      className={
        onLive
          ? "brand-gradient sticky top-0 z-20 text-white shadow-md shadow-primary/20"
          : "sticky top-0 z-20 border-b bg-background/90 backdrop-blur"
      }
    >
      <div className="app-container flex h-16 items-center">
        <Link
          href="/"
          aria-label="LiveQA ホーム"
          className="inline-flex items-center gap-2.5 transition-opacity hover:opacity-90"
        >
          <Image
            src={onLive ? "/brand/emblem-white.png" : "/brand/emblem-blue.png"}
            width={38}
            height={38}
            alt=""
            priority
          />
          <span className="text-xl font-bold leading-none tracking-tight">
            Live
            <span className={onLive ? "text-[#f0d287]" : "text-primary"}>QA</span>
          </span>
        </Link>
      </div>
    </header>
  );
}
