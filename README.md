# MeuApp — Android nativo (Kotlin)

Projeto Android mínimo, 100% Kotlin + View system nativo (sem Compose, sem frameworks híbridos), pronto para compilar automaticamente via GitHub Actions.

## Estrutura

```
MeuApp/
├── app/
│   ├── build.gradle.kts          # config do módulo app
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/meuapp/MainActivity.kt
│       └── res/...
├── build.gradle.kts               # config raiz
├── settings.gradle.kts
├── gradle.properties
└── .github/workflows/build.yml    # pipeline de build
```

## Como usar

1. Crie um repositório novo no GitHub.
2. Suba todo o conteúdo desta pasta para a raiz do repositório (mantenha a pasta `.github/workflows`).
3. Faça o push para a branch `main`.
4. Vá em **Actions** no GitHub → o workflow "Build APK" vai rodar automaticamente.
5. Ao terminar, baixe o APK em **Actions → (execução) → Artifacts → app-debug**.

Você também pode disparar manualmente pela aba Actions, usando o botão "Run workflow" (isso funciona graças ao `workflow_dispatch`).

## O que o workflow faz

- Configura JDK 17, Android SDK e Gradle automaticamente (não é necessário `gradlew` versionado no repositório).
- Compila um APK de **debug** (`assembleDebug`) em todo push/PR para `main`.
- Compila também um APK de **release não assinado** (`assembleRelease`) apenas em pushes para `main`.
- Publica os APKs como artifacts do GitHub Actions.

## Personalizações comuns

- **applicationId / nome do pacote**: altere em `app/build.gradle.kts` (`namespace` e `applicationId`) e mova o pacote em `app/src/main/java/...`.
- **Nome do app**: edite `app_name` em `app/src/main/res/values/strings.xml`.
- **Assinar o APK de release**: adicione seu keystore como *secret* do repositório e configure um bloco `signingConfigs` em `app/build.gradle.kts`, referenciando os secrets via variáveis de ambiente no workflow.
- **Versão do Android/Kotlin**: ajuste as versões dos plugins no `build.gradle.kts` raiz.

## Pareamento ADB via Wi-Fi (novo)

A tela principal agora tem uma seção para parear com a Depuração sem fio do Android (Android 11+):

1. No dispositivo-alvo: **Opções do desenvolvedor → Depuração sem fio → Parear dispositivo com código de pareamento**.
2. No app: toque em **"Procurar dispositivo automaticamente"** — ele usa mDNS/NSD para achar o IP e a porta do serviço de pareamento na rede local (ou digite manualmente, se a busca automática falhar).
3. Digite o **código de 6 dígitos** exibido na tela do dispositivo-alvo e toque em **"Parear e conectar"**.

Isso usa a biblioteca open source [libadb-android](https://github.com/MuntashirAkon/libadb-android), que implementa o protocolo de pareamento/TLS do ADB dentro do próprio app (sem precisar de PC).

**⚠️ Atenção ao compilar pela primeira vez:** não consegui confirmar 100% o pacote Java exato da classe `AbsAdbConnectionManager` nesta biblioteca (a documentação oficial não mostra o `import` completo). Usei `io.github.muntashirakon.adb.AbsAdbConnectionManager` em `AdbConnectionManager.kt` por ser o padrão mais provável, mas se o build falhar com "classe não encontrada", abra o repositório da lib no GitHub (aba "Code") para confirmar o pacote correto e ajuste apenas essa linha de `import`.

**Sobre uso:** essa função exige que você veja o código de pareamento na tela do próprio dispositivo (Configurações de desenvolvedor) — ou seja, serve para você conectar seus próprios aparelhos, não para acessar dispositivos de terceiros sem permissão.

## Rodando localmente (opcional)

Se quiser compilar na sua máquina, instale o Android Studio (que já traz o Gradle wrapper) ou tenha o Gradle instalado e rode:

```bash
gradle assembleDebug
```

O APK gerado fica em `app/build/outputs/apk/debug/app-debug.apk`.
