package com.othergram.otherredactor

data class Template(val id: String, val ru: String, val en: String, val descRu: String, val descEn: String, val code: String)

object Templates {
    val blank = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Document</title>
</head>
<body>

</body>
</html>
"""

    private val landing = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Landing</title>
  <style>
    * { box-sizing: border-box; margin: 0; }
    body { font-family: system-ui, sans-serif; background: #fdf6ec; color: #2b211b; }
    header { padding: 64px 24px; text-align: center;
      background: linear-gradient(135deg, #8c6d5a, #c4a386); color: #fff; }
    h1 { font-size: clamp(2rem, 6vw, 3.5rem); }
    header p { opacity: .85; margin-top: 12px; }
    .btn { display: inline-block; margin-top: 24px; padding: 14px 28px; border-radius: 999px;
      background: #fff; color: #6b4a38; text-decoration: none; font-weight: 700; }
    .grid { display: grid; gap: 16px; padding: 24px;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); }
    .card { background: #fff; border-radius: 24px; padding: 24px;
      box-shadow: 0 6px 24px rgba(107, 74, 56, .12); }
    .card h3 { margin-bottom: 8px; color: #6b4a38; }
  </style>
</head>
<body>
  <header>
    <h1>Hello, world!</h1>
    <p>A tiny landing page made in OtherRedactor</p>
    <a class="btn" href="#features">Learn more</a>
  </header>
  <section class="grid" id="features">
    <div class="card"><h3>Fast</h3><p>Loads instantly on any device.</p></div>
    <div class="card"><h3>Responsive</h3><p>Looks great on phones and desktops.</p></div>
    <div class="card"><h3>Simple</h3><p>Just HTML and CSS, nothing else.</p></div>
  </section>
</body>
</html>
"""

    private val counter = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Counter</title>
  <style>
    body { display: grid; place-items: center; min-height: 100vh; margin: 0;
      font-family: system-ui, sans-serif; background: #f3e5cc; }
    .box { text-align: center; }
    #value { font-size: 96px; font-weight: 800; color: #6b4a38; }
    button { font-size: 20px; padding: 12px 24px; margin: 6px; border: 0;
      border-radius: 16px; background: #8c6d5a; color: #fff; }
    button:active { transform: scale(.95); }
  </style>
</head>
<body>
  <div class="box">
    <div id="value">0</div>
    <button id="minus">−</button>
    <button id="plus">+</button>
  </div>
  <script>
    let count = 0;
    const value = document.getElementById('value');
    function render() {
      value.textContent = count;
      console.log('count =', count);
    }
    document.getElementById('plus').onclick = () => { count++; render(); };
    document.getElementById('minus').onclick = () => { count--; render(); };
  </script>
</body>
</html>
"""

    private val animation = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>CSS animation</title>
  <style>
    body { margin: 0; height: 100vh; display: grid; place-items: center; background: #19130f; }
    .blob { width: 160px; height: 160px; background: linear-gradient(135deg, #e9c2a6, #8c6d5a);
      animation: morph 6s ease-in-out infinite, spin 12s linear infinite; }
    @keyframes morph {
      0%, 100% { border-radius: 42% 58% 70% 30% / 45% 45% 55% 55%; }
      50% { border-radius: 70% 30% 46% 54% / 30% 29% 71% 70%; }
    }
    @keyframes spin { to { transform: rotate(360deg); } }
  </style>
</head>
<body>
  <div class="blob"></div>
</body>
</html>
"""

    private val canvas = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Canvas</title>
  <style> body { margin: 0; overflow: hidden; background: #fdf6ec; } </style>
</head>
<body>
  <canvas id="c"></canvas>
  <script>
    const c = document.getElementById('c');
    const ctx = c.getContext('2d');
    const dots = [];
    function resize() { c.width = innerWidth; c.height = innerHeight; }
    addEventListener('resize', resize); resize();
    addEventListener('pointermove', e => {
      dots.push({ x: e.clientX, y: e.clientY, r: 18, a: 1 });
    });
    (function loop() {
      ctx.clearRect(0, 0, c.width, c.height);
      for (const d of dots) {
        d.r *= 0.96; d.a *= 0.95;
        ctx.fillStyle = `rgba(140, 109, 90, ${'$'}{d.a})`;
        ctx.beginPath(); ctx.arc(d.x, d.y, d.r, 0, Math.PI * 2); ctx.fill();
      }
      while (dots.length && dots[0].a < 0.02) dots.shift();
      requestAnimationFrame(loop);
    })();
  </script>
</body>
</html>
"""

    val all = listOf(
        Template("blank", "Пустой HTML5", "Blank HTML5", "Чистый каркас страницы", "Clean page skeleton", blank),
        Template("landing", "Лендинг", "Landing page", "Шапка, кнопка и карточки", "Hero, button and cards", landing),
        Template("counter", "Счётчик на JS", "JS counter", "Кнопки, события, console.log", "Buttons, events, console.log", counter),
        Template("anim", "CSS-анимация", "CSS animation", "Морфинг и вращение", "Morph and spin keyframes", animation),
        Template("canvas", "Canvas", "Canvas", "Рисование пальцем", "Draw with your finger", canvas),
    )

    val sampleIndex = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>My site</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <main class="card">
    <h1>OtherRedactor</h1>
    <p>Edit <b>index.html</b>, <b>style.css</b> and <b>script.js</b>, then press ▶</p>
    <button id="hello">Say hello</button>
  </main>
  <script src="script.js"></script>
</body>
</html>
"""

    val sampleCss = """body {
  margin: 0;
  min-height: 100vh;
  display: grid;
  place-items: center;
  font-family: system-ui, sans-serif;
  background: #f3e5cc;
}

.card {
  padding: 32px;
  border-radius: 28px;
  background: #fff;
  box-shadow: 0 8px 32px rgba(107, 74, 56, 0.15);
  text-align: center;
}

h1 { color: #6b4a38; }

button {
  padding: 12px 24px;
  border: 0;
  border-radius: 999px;
  background: #8c6d5a;
  color: #fff;
  font-size: 16px;
}
"""

    val sampleJs = """document.getElementById('hello').addEventListener('click', () => {
  console.log('Hello from script.js!');
  alert('Hello! 👋');
});
"""
}
