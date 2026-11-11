# Kisi bhi HTML game me R7 ads lagane ka code (Mini Block World ke liye)

## Step 1 - game ko app me bundle karo
1. `Mini_Block_World.html` ko `app/src/main/assets/games/mini_block_world.html` naam se copy karo.
2. Isi folder me `mini_block_world_icon.webp` aur `mini_block_world_banner.webp` rakho.
3. `OfflineGames.kt` ki list `all` me ek line jodo:

   Local("Mini Block World", "Sandbox", "mini_block_world.html", listOf("miniblockworld", "blockworld")),

   (Sirf app ke andar bundled game ko ads ka bridge milta hai. Online link wale game ko nahi, ye security ke liye hai.)

## Step 2 - ye helper HTML ke <script> ke sabse upar ek baar paste karo
```js
/* ===== R7 Play ads helper (paste once) ===== */
function r7GameEnd() { try { window.R7Ads && R7Ads.gameEnd(); } catch (e) {} }
function r7AdReady() { try { return !!(window.R7Ads && R7Ads.hasRewarded()); } catch (e) { return false; } }
var r7Pending = {};
window.r7AdResult = function (tag, ok) { var f = r7Pending[tag]; delete r7Pending[tag]; if (f) f(ok); };
function r7Reward(tag, onReward) {            // returns false if no video is ready
  if (!r7AdReady()) return false;
  r7Pending[tag] = function (ok) { if (ok) onReward(); };
  R7Ads.showRewarded(tag);
  return true;
}
```

## Step 3 - kahan kya call karna hai
| Jagah | Code |
|---|---|
| Game WIN hone par (level clear / mission complete) | `r7GameEnd();` |
| Game OVER hone par (player mar gaya) | `r7GameEnd();` |
| "Revive / dobara jinda ho" button | `if (!r7Reward('revive', function(){ respawnPlayer(); })) { /* ad ready nahi: message dikhao */ }` |
| "Free coins / free item" button | `r7Reward('coins', function(){ coins += 100; saveGame(); });` |
| Inventory item 0 ho (jaise Nut Sort me) | `if (!r7Reward('item', function(){ item++; })) openShop();` |

- Interstitial automatically **har 2 game ke baad** aata hai, tumhe bas `r7GameEnd()` har win/over par call karna hai.
- Rewarded ka inaam sirf `onReward` function ke andar do (video poora dekhne ke baad hi chalta hai).
- Browser me test karte waqt `R7Ads` nahi hota, helper ke wajah se game bina error ke chalta rehta hai.

## ONLINE game (games.json wali link) me ads
1. Game ka page apne hi domain par ho (jaise `https://tumhara-naam.github.io/...`).
2. Us domain ka host `Config.kt` ki `AD_TRUSTED_HOSTS` list me jodo (sirf apna domain, kisi aur ka nahi).
3. Upar wala Step 2 ka helper aur Step 3 ke calls apne online HTML me bhi daalo. Browser me `R7Ads` nahi hota, to game bina error ke chalta hai.
4. Online game me agar pehle se koi dusri ad (AdSense wagairah) laga rakhi hai to hata do, warna double ads aur policy ka risk.
