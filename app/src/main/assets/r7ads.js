/* R7 Play ads library (v1). One file, works in the R7 Play app and (as a safe stub) in a normal browser.
 * The app injects it automatically into trusted online games; you may also include it yourself:
 *   <script src="r7ads.js"></script>
 */
(function (w) {
  if (w.R7Ads && w.R7Ads.__v) return;
  var NA = "Ad अभी उपलब्ध नहीं है। कृपया थोड़ी देर बाद कोशिश करें।";
  var NET = "कृपया अपना इंटरनेट कनेक्शन चेक करें।";
  var native = w.R7AdsNative;           // exists only inside R7 Play, only on trusted pages
  var pending = {}, seq = 0, handlers = {}, subscribed = false;

  function send(msg, cb) {
    if (!native || !native.postMessage) { if (cb) cb({ status: "unavailable", message: NA, notInApp: true }); return; }
    var id = "r" + (++seq) + "_" + Date.now().toString(36);
    msg.id = id;
    if (cb) pending[id] = cb;
    try { native.postMessage(JSON.stringify(msg)); }
    catch (e) { delete pending[id]; if (cb) cb({ status: "unavailable", message: NA }); }
  }

  if (native && native.addEventListener) {
    native.addEventListener("message", function (e) {
      var d; try { d = JSON.parse(e.data); } catch (x) { return; }
      if (d.id && pending[d.id]) { var f = pending[d.id]; delete pending[d.id]; try { f(d); } catch (x) {} }
      else if (d.event) {
        (handlers[d.event] || []).concat(handlers["*"] || []).forEach(function (fn) { try { fn(d); } catch (x) {} });
      }
    });
  }

  w.R7Ads = {
    __v: 1,
    inApp: !!native,

    /** Interstitial. opts (all optional): onClosed(), onUnavailable(message) */
    showInterstitial: function (opts) {
      opts = opts || {};
      send({ op: "interstitial" }, function (r) {
        if (r.status === "closed") { if (opts.onClosed) opts.onClosed(); }
        else if (opts.onUnavailable) opts.onUnavailable(r.message || NA);
      });
    },

    /** Rewarded. opts: rewardType, amount, onReward(result), onUnavailable(message), onSkip(message) (optional).
     *  onReward is called with result.success === true ONLY after Google's reward callback. */
    showRewarded: function (opts) {
      opts = opts || {};
      var amount = (opts.amount == null) ? 1 : opts.amount, done = false;
      send({ op: "rewarded" }, function (r) {
        if (done) return; done = true;
        if (r.status === "rewarded") {
          if (opts.onReward) opts.onReward({ success: true, rewardType: opts.rewardType || "", amount: amount });
        } else if (r.status === "skipped") {
          if (opts.onReward) opts.onReward({ success: false, reason: "skipped", rewardType: opts.rewardType || "", amount: 0 });
          if (opts.onSkip) opts.onSkip(r.message);
        } else if (opts.onUnavailable) {
          opts.onUnavailable(r.message || (r.status === "offline" ? NET : NA));
        }
      });
    },

    /** cb(true|false): does the phone have an internet connection */
    isOnline: function (cb) {
      if (!native) { if (cb) cb(w.navigator && w.navigator.onLine !== false); return; }
      send({ op: "isOnline" }, function (r) { if (cb) cb(!!r.online); });
    },

    /** kind: "rewarded" | "interstitial" | "banner"; cb(true|false) */
    isReady: function (kind, cb) {
      send({ op: "isReady", kind: kind }, function (r) { if (cb) cb(!!r.ready); });
    },

    /** event: "loaded" | "failed" | "*"; fn({event, kind}) */
    on: function (event, fn) {
      (handlers[event] = handlers[event] || []).push(fn);
      if (!subscribed && native) { subscribed = true; send({ op: "subscribe" }); }
    },

    messages: { adUnavailable: NA, noInternet: NET }
  };
})(window);
