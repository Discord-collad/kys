/* ============================================================
   KYS - Sidebar: abrir / cerrar / colapsar (lógica única y compartida)
   - Desktop (>768px): visible; hamburguesa alterna expandida <-> rail de iconos.
   - Móvil (<=768px): cerrada al inicio; hamburguesa la abre como overlay;
     se cierra tocando fuera, con Escape o al elegir un link.
   No guarda estado (sin localStorage): al recargar, desktop abierto y móvil cerrado.
   ============================================================ */
(function () {
    'use strict';

    var body = document.body;
    var sidebar = document.getElementById('sidebar');
    var hamburger = document.getElementById('hamburgerBtn');
    var backdrop = document.getElementById('sidebarBackdrop');
    if (!sidebar || !hamburger) return;

    function esMovil() { return window.matchMedia('(max-width: 768px)').matches; }

    function enMovilEstaAbierta() { return body.classList.contains('sidebar-movil-abierta'); }

    function abrirMovil() {
        body.classList.add('sidebar-movil-abierta');
        hamburger.setAttribute('aria-expanded', 'true');
        hamburger.classList.add('active');
    }

    function cerrarMovil() {
        body.classList.remove('sidebar-movil-abierta');
        hamburger.setAttribute('aria-expanded', 'false');
        hamburger.classList.remove('active');
    }

    function alternar() {
        if (esMovil()) {
            enMovilEstaAbierta() ? cerrarMovil() : abrirMovil();
        } else {
            var colapsada = body.classList.toggle('sidebar-colapsada');
            hamburger.setAttribute('aria-expanded', String(!colapsada));
            hamburger.classList.toggle('active', !colapsada);
        }
    }

    hamburger.addEventListener('click', alternar);

    // Inicializar estado: en móvil, cerrado por defecto
    if (esMovil()) {
        cerrarMovil();
    } else {
        // En desktop, expandido por defecto
        body.classList.remove('sidebar-colapsada');
        hamburger.setAttribute('aria-expanded', 'true');
    }

    if (backdrop) {
        backdrop.addEventListener('click', cerrarMovil);
    }

    // Cerrar al elegir un link (solo importa en móvil)
    sidebar.addEventListener('click', function (e) {
        var link = e.target.closest ? e.target.closest('a.side-link') : null;
        if (link && esMovil()) cerrarMovil();
    });

    // Escape cierra el menú móvil
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && esMovil() && enMovilEstaAbierta()) cerrarMovil();
    });

    // Al pasar de móvil a desktop, limpiar estado móvil
    window.matchMedia('(max-width: 768px)').addEventListener('change', function (mq) {
        if (!mq.matches) cerrarMovil();
    });
})();
