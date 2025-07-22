# Kirikata_Sensei
procon36 における「きりかた先生」のAndroid開発用のリポジトリ。

githubの使い方
① mainを最新化する（作業前）
git checkout main
git pull origin main

② ブランチを切る（新しい作業用ブランチ作成）
git checkout -b feature/作業名_作業者名

作業する

③ ファイルを編集 → コミット
git add .
git commit -m "例： ログイン機能追加"

④ ブランチをGitHubにPush
git push -u origin feature/作業名_作業者名

⑤ GitHubでPull Request（PR）作成
GitHubを開くと「Compare & pull request」ボタンが出るのでクリック。
（出ない場合は「Pull requests → New pull request」から）
base: main / compare: feature/作業名_作業者名を選ぶ。
タイトル＆説明を書き、「Create pull request」を押す。
