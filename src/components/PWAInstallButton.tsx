import React, { useState } from 'react';
import { Download, Share, Smartphone, X } from 'lucide-react';
import { usePWAInstall } from '../hooks/usePWAInstall';

export const PWAInstallButton: React.FC = () => {
  const { isInstallable, isInstalled, isIOS, install } = usePWAInstall();
  const [showIOSGuide, setShowIOSGuide] = useState(false);

  if (isInstalled) {
    return null;
  }

  if (isInstallable) {
    return (
      <button
        type="button"
        onClick={install}
        className="inline-flex min-h-[40px] items-center gap-2 rounded-xl border border-indigo-400/30 bg-indigo-500/20 px-3.5 py-2 text-xs font-medium text-indigo-100 backdrop-blur-md hover:bg-indigo-500/30 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-400 active:scale-[0.97] transition whitespace-nowrap shrink-0"
      >
        <Download className="h-4 w-4 text-sky-300" aria-hidden="true" />
        <span>نصب روی گوشی</span>
      </button>
    );
  }

  if (isIOS) {
    return (
      <>
        <button
          type="button"
          onClick={() => setShowIOSGuide(true)}
          className="inline-flex min-h-[40px] items-center gap-2 rounded-xl border border-white/15 bg-white/[0.07] px-3.5 py-2 text-xs font-medium text-zinc-100 backdrop-blur-md hover:bg-white/[0.12] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sky-400 transition whitespace-nowrap shrink-0"
        >
          <Smartphone className="h-4 w-4 text-sky-300" aria-hidden="true" />
          <span>نصب در آیفون</span>
        </button>

        {showIOSGuide && (
          <div
            role="dialog"
            aria-modal="true"
            aria-label="راهنمای نصب روی آیفون"
            className="fixed inset-0 z-50 flex items-end justify-center bg-black/70 p-0 backdrop-blur-sm sm:items-center sm:p-4"
          >
            <div className="w-full max-w-sm rounded-t-3xl border border-white/15 bg-zinc-900/95 p-6 text-zinc-100 backdrop-blur-xl sm:rounded-2xl">
              <div className="flex items-center justify-between gap-3">
                <h3 className="text-base font-semibold">نصب آواپلیر روی آیفون</h3>
                <button
                  type="button"
                  onClick={() => setShowIOSGuide(false)}
                  aria-label="بستن راهنما"
                  className="flex h-10 w-10 items-center justify-center rounded-xl text-zinc-400 hover:bg-white/10 hover:text-white"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>
              <ol className="mt-4 space-y-3 text-sm leading-6 text-zinc-300">
                <li className="flex items-start gap-2.5">
                  <span className="mt-0.5 font-mono text-xs font-semibold text-sky-400">۱.</span>
                  <span>
                    در نوار پایین مرورگر Safari دکمه{' '}
                    <strong className="inline-flex items-center gap-1 text-white">
                      Share <Share className="inline h-3.5 w-3.5" aria-hidden="true" />
                    </strong>{' '}
                    را بزنید.
                  </span>
                </li>
                <li className="flex items-start gap-2.5">
                  <span className="mt-0.5 font-mono text-xs font-semibold text-sky-400">۲.</span>
                  <span>
                    به پایین اسکرول کنید و گزینه{' '}
                    <strong className="text-white" dir="ltr">
                      Add to Home Screen
                    </strong>{' '}
                    را انتخاب کنید.
                  </span>
                </li>
              </ol>
              <button
                type="button"
                onClick={() => setShowIOSGuide(false)}
                className="mt-6 min-h-[44px] w-full rounded-xl bg-indigo-600 py-2.5 text-sm font-medium text-white hover:bg-indigo-500 transition-colors"
              >
                متوجه شدم
              </button>
            </div>
          </div>
        )}
      </>
    );
  }

  return null;
};
