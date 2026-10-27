"""Original periodic machine loops; authoring only, never part of the mod runtime."""
from pathlib import Path
import json
import sys

sys.path.insert(0, str(Path(__file__).parent / "python-deps"))
import numpy as np
import soundfile as sf

output = Path(sys.argv[1])
output.mkdir(parents=True, exist_ok=True)
rate, seconds = 24000, 3
count = rate * seconds
t = np.arange(count) / rate
frequency = np.fft.rfftfreq(count, 1 / rate)

for name, fundamental, pulse, seed in (("pump_running", 60, 3, 7101), ("compressor_running", 48, 6, 7102)):
    rng = np.random.default_rng(seed)
    envelope = (frequency / 300) * np.exp(-frequency / 700)
    envelope[frequency < 90] = 0
    noise = np.fft.irfft(envelope * np.exp(2j * np.pi * rng.random(len(frequency))), n=count)
    noise /= np.sqrt(np.mean(noise ** 2))
    motor = (np.sin(2 * np.pi * fundamental * t)
             + .25 * np.sin(2 * np.pi * 2 * fundamental * t + .4)
             + .12 * np.sin(2 * np.pi * 4 * fundamental * t + .9))
    if name.startswith("pump"):
        sound = .12 * motor + .028 * noise * (1 + .12 * np.cos(2 * np.pi * pulse * t))
    else:
        sound = (.14 * motor + .035 * noise) * (1 + .28 * np.cos(2 * np.pi * pulse * t))
    sound -= np.mean(sound)
    path = output / (name + ".ogg")
    sf.write(path, sound, rate, format="OGG", subtype="VORBIS")
    decoded, decoded_rate = sf.read(path)
    print(json.dumps(dict(asset=name, bytes=path.stat().st_size, frames=len(decoded), rate=decoded_rate,
                          rms=float(np.sqrt(np.mean(decoded ** 2))), peak=float(np.max(np.abs(decoded))),
                          seam_step=float(abs(decoded[0] - decoded[-1])))))
