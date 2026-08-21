import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import JoinForm from "./JoinForm";

export default function Join(){

    return(
        <div className="flex min-h-screen items-center justify-center px-4 py-10">
            <Card className="w-full max-w-md">
                <CardHeader>
                    <CardTitle className="text-xl">イベント参加コード入力</CardTitle>
                    <CardDescription>
                        HOSTより紹介のあるコードを入力してください。
                    </CardDescription>
                </CardHeader>
                <CardContent>
                    <JoinForm />
                </CardContent>
            </Card>
        </div>
    );
}