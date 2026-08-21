import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Logo } from "@/components/brand/Logo";
import { RiArrowRightLine, RiLoginBoxLine } from "@remixicon/react";
import Link from "next/link";

/**
 * HOSTログイン画面の表示
 * @returns ログインホーム画面の表示
 */

export default function Home() {
  const LOGIN_URL =
    process.env.NEXT_PUBLIC_KEYCLOAK_LOGIN_URL ??
    "http:localhost:8081/oauth2/authorization/keycloak";

  return (
    <div className="flex min-h-screen flex-col">
      {/* Header */}
      <header className="sticky top-0 z-10 border-b bg-background/80 backdrop-blur">
        <div className="app-container flex h-16 items-center justify-between">
          <Link href="/" aria-label="LiveQA ホーム">
            <Logo size={34} />
          </Link>
          <nav className="flex items-center gap-2">
            <Button
              variant="outline"
              nativeButton={false}
              render={<Link href="/join" />}
            >
              コードで参加
            </Button>
          </nav>
        </div>
      </header>

      {/* Hero */}
      <main className="app-container flex flex-1 items-center py-12 sm:py-20">
        <div className="grid w-full items-center gap-12 lg:grid-cols-2">
          {/* Left: copy + login */}
          <div className="space-y-8">
            <div className="space-y-4">
              <span className="inline-flex items-center gap-2 rounded-full bg-accent/15 px-3 py-1 text-sm font-medium text-accent-foreground ring-1 ring-accent/30">
                Live Question &amp; Answer
              </span>
              <h1 className="text-4xl font-bold leading-tight tracking-tight sm:text-5xl">
                会場の声を、
                <br />
                <span className="text-primary">リアルタイム</span>に。
              </h1>
              <p className="max-w-md text-lg text-muted-foreground">
                イベントやセミナーで参加者からの質問を集め、投票で盛り上げる。
                ログインして、あなたのライブを始めましょう。
              </p>
            </div>

            <Card className="max-w-md">
              <CardHeader>
                <CardTitle>ホストとしてログイン</CardTitle>
                <CardDescription>
                  ログインするとイベントを作成・開催できます。
                </CardDescription>
              </CardHeader>
              <CardContent className="flex flex-col gap-3 sm:flex-row">
                <Button
                  size="lg"
                  nativeButton={false}
                  render={<a href={LOGIN_URL} />}
                  className="w-full sm:w-auto"
                >
                  <RiLoginBoxLine className="size-4" />
                  ログイン
                </Button>
                <Button
                  size="lg"
                  variant="outline"
                  nativeButton={false}
                  render={<Link href="/join" />}
                  className="w-full sm:w-auto"
                >
                  参加コードを持っている
                  <RiArrowRightLine className="size-4" />
                </Button>
              </CardContent>
            </Card>
          </div>

          {/* Right: brand visual */}
          <div className="relative hidden lg:block">
            <div className="brand-gradient relative aspect-square overflow-hidden rounded-4xl p-10 shadow-2xl shadow-primary/30">
              <div className="flex h-full flex-col items-center justify-center gap-6 text-center text-primary-foreground">
                <Logo size={96} showWordmark={false} className="scale-125" />
                <div>
                  <p className="text-5xl font-black tracking-tight">LiveQA</p>
                  <p className="mt-2 text-sm font-semibold uppercase tracking-[0.3em] text-accent">
                    Live Question &amp; Answer
                  </p>
                </div>
              </div>
              {/* decorative bubbles */}
              <span className="absolute -right-6 top-10 size-24 rounded-3xl bg-white/10 blur-xl" />
              <span className="absolute -left-4 bottom-12 size-32 rounded-full bg-accent/20 blur-2xl" />
            </div>
          </div>
        </div>
      </main>

      <footer className="border-t">
        <div className="app-container flex h-14 items-center text-sm text-muted-foreground">
          © {new Date().getFullYear()} LiveQA
        </div>
      </footer>
    </div>
  );
}
