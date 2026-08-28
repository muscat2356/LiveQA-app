/**
 * APi呼び出しの失敗を表す例外
 * HTTPステータスを保持するため、呼び出し側で、
 * 404/409などを分岐
 */
export class ApiError extends Error{
    readonly status: number;

    constructor(status: number, message: string){
        super(message);
        this.name = "ApiError";
        this.status = status;
    }

}