import Image from "next/image";
import Link from "next/link";

/**
 * 全画面共通ヘッダー（ブランド青帯 + 切り出しエンブレム + ワードマーク）
 */
export function AppHeader() {
  return (
    <header className="brand-gradient sticky top-0 z-20 text-white shadow-md shadow-primary/20">
      <div className="app-container flex h-16 items-center">
        <Link
          href="/"
          aria-label="LiveQA ホーム"
          className="inline-flex items-center gap-2.5 transition-opacity hover:opacity-90"
        >
          <Image src="/brand/emblem-white.png" width={38} height={38} alt="" priority />
          <span className="text-xl font-bold leading-none tracking-tight">
            Live<span className="text-[#f0d287]">QA</span>
          </span>
        </Link>
      </div>
    </header>
  );
}
