"use client"

import useUserRegister from "../../components/user/useUserRegister";

export default function RegisterPage(){
    const { email, setEmail, pending, error, done, register} = useUserRegister();

    if(done){
        return(
            <main>
                <h1>登録を受け付けました</h1>
                <p>{email}宛にパスワード設定用の通知を送信しました。</p>
                <p>メールが届かない場合には、迷惑メールフォルダをご確認ください</p>
            </main>
        )
    }

    return(
        <main>
            <h1>主催者登録</h1>

            <form onSubmit={(e) => {
                e.preventDefault()
                register()
            }}>

                <label htmlFor="email">メールアドレス</label>

                <input 
                    type="email" 
                    id="email"
                    name="email"
                    required
                    autoComplete="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    disabled={pending}
                    aria-invalid={error ? true: undefined}
                    aria-describedby={error ? "email-error" : undefined}
                />

                {error && (
                    <p id="email-error" role="alert">
                        {error}
                    </p>
                )}

                <button type="submit" disabled={pending || email === ""}>
                    {pending ? "送信中..." : "登録"}
                </button>
            </form>
        </main>
    )
}