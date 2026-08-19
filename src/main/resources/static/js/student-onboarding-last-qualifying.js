(function () {
    function numberValue(field) {
        if (!field) return null;
        const raw = (field.value || "").trim();
        if (!raw) return null;
        const parsed = Number(raw);
        return Number.isFinite(parsed) ? parsed : null;
    }

    function updatePercentage(panel) {
        const obtained = panel.querySelector('[data-lastq-field="obtainedMarks"]');
        const total = panel.querySelector('[data-lastq-field="totalMarks"]');
        const percentage = panel.querySelector('[data-lastq-field="percentage"]');
        if (!percentage) return;
        const obtainedValue = numberValue(obtained);
        const totalValue = numberValue(total);
        if (obtainedValue == null || totalValue == null || totalValue <= 0) {
            percentage.value = "";
            return;
        }
        const value = Math.round(((obtainedValue / totalValue) * 100) * 100) / 100;
        percentage.value = value % 1 === 0 ? String(Math.trunc(value)) : value.toFixed(2);
    }

    document.addEventListener("DOMContentLoaded", function () {
        const panel = document.querySelector('[data-step-panel="last-qualifying"]');
        if (!panel) return;

        const obtained = panel.querySelector('[data-lastq-field="obtainedMarks"]');
        const total = panel.querySelector('[data-lastq-field="totalMarks"]');
        if (obtained) {
            obtained.addEventListener("input", function () { updatePercentage(panel); });
            obtained.addEventListener("change", function () { updatePercentage(panel); });
        }
        if (total) {
            total.addEventListener("input", function () { updatePercentage(panel); });
            total.addEventListener("change", function () { updatePercentage(panel); });
        }
        updatePercentage(panel);
    });
})();
