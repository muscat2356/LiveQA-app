import { RiQuestionAnswerFill } from "@remixicon/react";
import { cn } from "@/lib/utils/utils";

type LogoProps = {
  /** アイコンタイルのサイズ(px) */
  size?: number;
  /** ワードマーク("LiveQA")を表示するか */
  showWordmark?: boolean;
  className?: string;
};

/**
 * LiveQA ブランドロゴ（吹き出しアイコン + ワードマーク）
 */
export function Logo({ size = 36, showWordmark = true, className }: LogoProps) {
  return (
    <span className={cn("inline-flex items-center gap-2.5", className)}>
      <span
        className="brand-tile shrink-0"
        style={{ width: size, height: size }}
        aria-hidden
      >
        <RiQuestionAnswerFill style={{ width: size * 0.58, height: size * 0.58 }} />
      </span>
      {showWordmark && (
        <span className="text-xl font-bold tracking-tight leading-none">
          Live<span className="text-primary">QA</span>
        </span>
      )}
    </span>
  );
}
