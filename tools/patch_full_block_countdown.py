#!/usr/bin/env python3
"""Attach a live hourly countdown without changing Compose callbacks or exit gating."""
from pathlib import Path
import sys
from patch_three_second_lock import method_replace, insert_before_return


def main(root):
    paths = list(root.rglob('LockActivity.smali'))
    if len(paths) != 1:
        raise RuntimeError('Expected one LockActivity')
    p = paths[0]
    text = p.read_text()
    if 'FullBlockCountdownView' in text:
        raise RuntimeError('Countdown already attached')
    if 'vxUnlockAt:J' not in text:
        raise RuntimeError('Existing three-second gate must remain installed')
    text = method_replace(text, 'private final render(Landroid/content/Intent;)V',
        lambda m: insert_before_return(m,
            '    invoke-static {p0}, Lcom/v1f/app/patch/FullBlockCountdownView;->attach(Landroid/app/Activity;)V\n\n'))
    for lifecycle, action in [('onStart', 'attach'), ('onStop', 'stop')]:
        if lifecycle + '()V' in text:
            raise RuntimeError('Unexpected existing lifecycle: ' + lifecycle)
        text += f'''
.method protected {lifecycle}()V
    .locals 0
    invoke-super {{p0}}, Landroidx/activity/ComponentActivity;->{lifecycle}()V
    invoke-static {{p0}}, Lcom/v1f/app/patch/FullBlockCountdownView;->{action}(Landroid/app/Activity;)V
    return-void
.end method
'''
    p.write_text(text)
    print('Live hourly countdown attached; render refresh and start/stop lifecycle wired')


if __name__ == '__main__':
    main(Path(sys.argv[1]))
