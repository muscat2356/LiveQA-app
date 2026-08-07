import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import EventForm from "./eventform";

export default function CreateNewEvent(){


    return(
        <div>
            <Card>
                <CardHeader>
                    <CardTitle>イベント新規作成</CardTitle>
                    <CardDescription>
                        イベントを作成するとURLと参加コードが発行されます。
                    </CardDescription>
                </CardHeader>
                <CardContent>
                    <EventForm />
                </CardContent>
            </Card>
        </div>
    );
}