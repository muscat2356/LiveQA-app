"use client"

import useUserRegister from "@/components/user/useUserRegister";
import { Button } from "@/components/ui/button";
import {
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export default function RegisterPage(){
    const { email, setEmail, pending, error, done, register} = useUserRegister();

    if(done){
        return(
            <div className="flex min-h-screen items-center justify-center px-4 py-10">
                <Card className="w-full max-w-md text-center">
                    <CardHeader className="items-center">
                        <CardTitle className="text-xl">登録を受け付けました</CardTitle>
                        <CardDescription>{email}宛にパスワード設定用の通知を送信しました。</CardDescription>
                    </CardHeader>
                    <CardContent>
                        <p className="text-sm text-muted-foreground">メールが届かない場合には、迷惑メールフォルダをご確認ください</p>
                    </CardContent>
                </Card>
            </div>
        )
    }

    return(
        <div className="flex min-h-screen items-center justify-center px-4 py-10">
            <Card className="w-full max-w-md">
                <CardHeader>
                    <CardTitle className="text-xl">主催者登録</CardTitle>
                    <CardDescription>
                        メールアドレスを登録すると、パスワード設定用の通知が届きます。
                    </CardDescription>
                </CardHeader>
                <CardContent>
                    <form
                        onSubmit={(e) => {
                            e.preventDefault()
                            register()
                        }}
                        className="space-y-4"
                    >
                        <div className="space-y-1.5">
                            <Label htmlFor="email">メールアドレス</Label>
                            <Input
                                type="email"
                                id="email"
                                name="email"
                                required
                                autoComplete="email"
                                placeholder="you@example.com"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                disabled={pending}
                                aria-invalid={error ? true: undefined}
                                aria-describedby={error ? "email-error" : undefined}
                                className="h-11"
                            />
                        </div>

                        {error && (
                            <p id="email-error" role="alert" className="text-sm text-destructive">
                                {error}
                            </p>
                        )}

                        <Button type="submit" size="lg" disabled={pending || email === ""} className="h-11 w-full">
                            {pending ? "送信中..." : "登録"}
                        </Button>
                    </form>
                </CardContent>
            </Card>
        </div>
    )
}
