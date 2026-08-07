import { Button } from "@/components/ui/button";
import { Card, CardAction, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import Image from "next/image";
import Link from "next/link";

/**
 * HOSTログイン画面の表示
 * @returns ログインホーム画面の表示
 */

export default function Home() {

  const LOGIN_URL = process.env.NEXT_PUBLIC_KEYCLOAK_LOGIN_URL ?? "http:localhost:8081/oauth2/authorization/keycloak"

  return (
    <div>
      <header className="border-b">
        <div className="mx-auto flex h-14 max-w-4xl items-center justify-between px-4">
          <Link href="/dashboard" className="font-semibold">
            LiveQA
          </Link>
          <nav className="flex items-center gap-2">
          </nav>
        </div>
      </header>
      <Card>
        <CardHeader>
          <CardTitle>ログイン画面</CardTitle>
          <CardDescription>ログインしてLiveを開催できます</CardDescription>
        </CardHeader>
        <CardContent>
          <Button>
            <a href={`${LOGIN_URL}`}>ログイン</a>
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
