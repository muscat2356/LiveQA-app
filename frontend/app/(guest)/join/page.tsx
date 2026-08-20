import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import JoinForm from "./JoinForm";

export default function Join(){

    return(
        <div>
            <Card>
                <CardHeader>
                    <CardTitle>イベント参加コード入力</CardTitle>
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