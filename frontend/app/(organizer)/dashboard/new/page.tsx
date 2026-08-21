import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import EventForm from "./eventform";

export default function CreateNewEvent(){


    return(
        <div className="app-container flex max-w-xl flex-col justify-center px-4 py-10 sm:py-16">
            <Card>
                <CardHeader>
                    <CardTitle className="text-xl">イベント新規作成</CardTitle>
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