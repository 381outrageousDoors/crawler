// Shared theme: apply the saved preference before first paint (no light flash
// under dark mode), then wire the #theme-toggle button once the DOM is ready.
(function () {
    var t = localStorage.getItem('theme');
    var dark = t ? t === 'dark' : window.matchMedia('(prefers-color-scheme: dark)').matches;
    document.documentElement.classList.toggle('dark', dark);

    document.addEventListener('DOMContentLoaded', function () {
        var btn = document.getElementById('theme-toggle');
        if (!btn) return;
        function sync() {
            btn.textContent = document.documentElement.classList.contains('dark') ? 'Light mode' : 'Dark mode';
        }
        btn.addEventListener('click', function () {
            var d = !document.documentElement.classList.contains('dark');
            document.documentElement.classList.toggle('dark', d);
            localStorage.setItem('theme', d ? 'dark' : 'light');
            sync();
        });
        sync();
    });
})();
