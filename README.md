# Comuinonek

A community app: create public or private communities, join by browsing (public) or by join code (private), and post inside them. Built with native Android (Kotlin) + Firebase (Auth + Firestore), meant to be built from the command line in Termux.

## What's included
- Email/password sign up & login (Firebase Auth)
- Create a community: name, description, public or private
- Private communities get an auto-generated 6-character join code you share manually
- "Discover" tab lists all public communities; "My Communities" tab lists ones you've joined
- Join a private community by entering its code
- Simple text post feed inside each community

## 1. One-time Firebase setup (do this on any browser, phone is fine)
1. Go to https://console.firebase.google.com and create a new project (any name).
2. In the project, click **Add app → Android**.
3. Package name must be exactly: `com.plszynon.comuinonek`
4. Download the `google-services.json` file it gives you.
5. Put that file at `app/google-services.json` in this project (replacing nothing — the file doesn't exist yet, you're adding it).
6. In the Firebase console: **Build → Authentication → Sign-in method → enable "Email/Password"**.
7. In the Firebase console: **Build → Firestore Database → Create database** (start in production mode, pick any region).
8. In Firestore, go to the **Rules** tab and paste in the contents of `firestore.rules` from this repo, then click **Publish**.

Without steps 3–8 the app will build but crash or silently fail on login/community actions.

## 2. Termux setup (on your phone)
```bash
pkg update && pkg upgrade
pkg install openjdk-17 git wget unzip -y
```

Install the Android command-line SDK tools (needed for `aapt2`, platform jars, etc.):
```bash
cd ~
mkdir -p android-sdk/cmdline-tools
cd android-sdk/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip
mv cmdline-tools latest
```

Add these to `~/.bashrc` (or `~/.zshrc`), then `source` it:
```bash
export ANDROID_HOME=$HOME/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
```

Install the SDK packages this project needs, accepting licenses:
```bash
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

Install Gradle (Termux ships a recent version):
```bash
pkg install gradle -y
```

> Note: `distributionUrl` in `gradle/wrapper/gradle-wrapper.properties` points to Gradle 8.7, but this repo does **not** include the `gradle-wrapper.jar` binary (binaries don't travel well through this workflow). Just use the `gradle` command you installed via `pkg install gradle` instead of `./gradlew` — the commands below use plain `gradle`.

## 3. Get the code onto your phone
If you already made the GitHub repo `plszynon/Cmm` (seen in your screenshot), either:
- rename it to `Comuinonek` on GitHub and push this code into it, or
- create a fresh repo called `Comuinonek` and push this into that.

```bash
cd ~
git clone https://github.com/plszynon/Comuinonek.git
cd Comuinonek
# copy in all the project files from this delivery, then:
cp /path/to/downloaded/google-services.json app/google-services.json
git add .
git commit -m "Initial Comuinonek app"
git push
```

## 4. Build the APK
```bash
cd ~/Comuinonek
gradle assembleDebug
```

The APK will be at:
```
app/build/outputs/apk/debug/app-debug.apk
```

Install it directly on the phone you're building on:
```bash
pkg install termux-api -y   # if not already installed
termux-open app/build/outputs/apk/debug/app-debug.apk
```
That opens the system installer — tap install (you may need to allow "install unknown apps" for Termux once).

## Data model (Firestore)
```
communities/{communityId}
  name, description, isPrivate, joinCode, ownerId, ownerName, memberCount, createdAt
  members/{uid} -> { uid, name, joinedAt }
  posts/{postId} -> { authorId, authorName, text, createdAt }

users/{uid}/myCommunities/{communityId} -> { name, isPrivate, role }
```

## Known limitations / good next steps
- No profile pictures or push notifications yet.
- Private communities are discoverable by anyone who has the exact join code but aren't listed anywhere — that's intentional (invite-only).
- No admin/moderation tools yet (kicking members, deleting posts by others, transferring ownership).
- `memberCount` is a simple counter; at real scale you'd want a Cloud Function to keep it accurate instead of client-side increments.
