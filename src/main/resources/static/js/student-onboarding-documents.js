(function () {
    function fileLabel(file) {
        return file ? file.name : "";
    }

    function humanSize(bytes) {
        const value = Number(bytes);
        if (!Number.isFinite(value) || value <= 0) return "";
        const kb = value / 1024;
        return kb.toFixed(kb >= 10 ? 0 : 1) + " KB";
    }

    function fileExtension(nameOrUrl) {
        const clean = (nameOrUrl || "").split("?")[0].split("#")[0];
        const index = clean.lastIndexOf(".");
        return index >= 0 ? clean.substring(index + 1).toLowerCase() : "";
    }

    function previewType(doc) {
        const extension = fileExtension(doc.fileName || doc.url || "");
        if (["jpg", "jpeg", "png"].includes(extension)) return "image";
        if (extension === "pdf") return "pdf";
        if (doc.file && doc.file.type) {
            if (doc.file.type.startsWith("image/")) return "image";
            if (doc.file.type === "application/pdf") return "pdf";
        }
        return "unsupported";
    }

    function ensurePreviewModal() {
        let modal = document.getElementById("studentDocumentPreviewModal");
        if (modal) return modal;

        modal = document.createElement("div");
        modal.id = "studentDocumentPreviewModal";
        modal.className = "doc-preview-modal";
        modal.innerHTML = `
            <div class="doc-preview-backdrop" data-doc-preview-close></div>
            <div class="doc-preview-dialog" role="dialog" aria-modal="true" aria-labelledby="studentDocumentPreviewTitle">
                <div class="doc-preview-head">
                    <div>
                        <div class="doc-preview-kicker">Document Preview</div>
                        <h3 id="studentDocumentPreviewTitle">Preview</h3>
                    </div>
                    <div class="doc-preview-controls">
                        <button type="button" data-doc-preview-zoom="out" title="Zoom out">-</button>
                        <button type="button" data-doc-preview-zoom="in" title="Zoom in">+</button>
                        <button type="button" data-doc-preview-close title="Close">x</button>
                    </div>
                </div>
                <div class="doc-preview-body">
                    <div class="doc-preview-loading">Loading preview...</div>
                    <div class="doc-preview-error" hidden>Unable to preview document</div>
                    <div class="doc-preview-stage"></div>
                </div>
            </div>`;
        const style = document.createElement("style");
        style.textContent = `
            .doc-preview-modal{position:fixed;inset:0;z-index:9999;display:none;align-items:center;justify-content:center;padding:18px}
            .doc-preview-modal.is-open{display:flex}
            .doc-preview-backdrop{position:absolute;inset:0;background:rgba(15,23,42,.64);backdrop-filter:blur(2px)}
            .doc-preview-dialog{position:relative;width:min(980px,96vw);height:min(760px,90vh);background:#fff;border-radius:8px;box-shadow:0 24px 80px rgba(15,23,42,.36);display:flex;flex-direction:column;overflow:hidden}
            .doc-preview-head{display:flex;justify-content:space-between;gap:12px;align-items:center;padding:13px 16px;border-bottom:1px solid #e2e8f0;background:#f8fafc}
            .doc-preview-kicker{font-size:10px;text-transform:uppercase;letter-spacing:.12em;color:#64748b;font-weight:800}
            .doc-preview-head h3{margin:2px 0 0;font-size:15px;color:#172033;line-height:1.25}
            .doc-preview-controls{display:flex;align-items:center;gap:8px}
            .doc-preview-controls button{width:32px;height:32px;border:1px solid #cbd5e1;background:#fff;border-radius:6px;color:#1f2937;font-weight:800;cursor:pointer}
            .doc-preview-body{position:relative;flex:1;background:#eef2f7;overflow:auto;display:flex;align-items:center;justify-content:center;padding:16px}
            .doc-preview-stage{width:100%;height:100%;display:flex;align-items:center;justify-content:center}
            .doc-preview-stage img{max-width:100%;max-height:100%;object-fit:contain;transition:transform .16s ease;transform-origin:center center}
            .doc-preview-stage iframe{width:100%;height:100%;border:0;background:#fff;border-radius:4px}
            .doc-preview-loading,.doc-preview-error{position:absolute;top:16px;left:50%;transform:translateX(-50%);padding:8px 12px;border-radius:999px;font-size:12px;font-weight:700;background:#fff;color:#334155;box-shadow:0 8px 24px rgba(15,23,42,.12)}
            .doc-preview-error{color:#b42318;border:1px solid #fecaca}
            @media (max-width: 720px){.doc-preview-modal{padding:8px}.doc-preview-dialog{width:100vw;height:94vh}.doc-preview-head{padding:10px 12px}.doc-preview-controls button{width:30px;height:30px}}
        `;
        document.head.appendChild(style);
        document.body.appendChild(modal);
        return modal;
    }

    document.addEventListener("DOMContentLoaded", function () {
        const panel = document.querySelector('[data-step-panel="doc-upload"]');
        if (!panel) return;

        const form = panel.closest("form");
        const nameField = panel.querySelector('[data-doc-field="name"]');
        const fileField = panel.querySelector('[data-doc-field="file"]');
        const addButton = panel.querySelector("[data-doc-add]");
        const listBody = panel.querySelector("[data-doc-list]");
        const maxSize = 200 * 1024;
        const docs = [];
        const objectUrls = new Map();
        let docCounter = 0;
        let previewZoom = 1;

        function syncState() {
            const serialized = JSON.stringify(docs.filter(doc => !doc.existing));
            const allSerialized = JSON.stringify(docs.map(doc => ({
                name: doc.name,
                fileName: doc.fileName,
                fileSize: doc.fileSize,
                status: doc.status || "PENDING",
                existing: Boolean(doc.existing)
            })));
            if (form) {
                form.dataset.docList = serialized;
            }
            if (panel) {
                panel.dataset.docCount = String(docs.length);
                panel.dataset.docList = allSerialized;
                panel.dataset.newDocList = serialized;
            }
        }

        function docUrl(doc) {
            if (doc.url) return doc.url;
            if (!doc.file) return "";
            if (!objectUrls.has(doc.token)) {
                objectUrls.set(doc.token, URL.createObjectURL(doc.file));
            }
            return objectUrls.get(doc.token);
        }

        function attachHiddenInputs(doc) {
            if (!form || !doc || !doc.file || doc.existing) return;
            const wrapper = document.createElement("div");
            wrapper.hidden = true;
            wrapper.dataset.docToken = doc.token;

            const nameInput = document.createElement("input");
            nameInput.type = "hidden";
            nameInput.name = "documentNames";
            nameInput.value = doc.name;

            const fileInput = document.createElement("input");
            fileInput.type = "file";
            fileInput.name = "documentFiles";

            try {
                const transfer = new DataTransfer();
                transfer.items.add(doc.file);
                fileInput.files = transfer.files;
            } catch (error) {
                return;
            }

            wrapper.appendChild(nameInput);
            wrapper.appendChild(fileInput);
            form.appendChild(wrapper);
            doc.hiddenWrapper = wrapper;
        }

        function removeHiddenInputs(doc) {
            if (doc && doc.hiddenWrapper && doc.hiddenWrapper.parentNode) {
                doc.hiddenWrapper.parentNode.removeChild(doc.hiddenWrapper);
            }
            if (doc && objectUrls.has(doc.token)) {
                URL.revokeObjectURL(objectUrls.get(doc.token));
                objectUrls.delete(doc.token);
            }
            if (doc) {
                doc.hiddenWrapper = null;
            }
        }

        function setInvalid(field, on) {
            if (!field) return;
            field.style.borderColor = on ? "#d92d20" : "#c7d0da";
            field.style.background = on ? "#fff7f7" : "#eaf1fb";
        }

        function openPreview(doc) {
            const modal = ensurePreviewModal();
            const title = modal.querySelector("#studentDocumentPreviewTitle");
            const stage = modal.querySelector(".doc-preview-stage");
            const loading = modal.querySelector(".doc-preview-loading");
            const error = modal.querySelector(".doc-preview-error");
            const zoomButtons = modal.querySelectorAll("[data-doc-preview-zoom]");
            const url = docUrl(doc);
            const type = previewType(doc);

            previewZoom = 1;
            title.textContent = doc.name || doc.fileName || "Document";
            stage.innerHTML = "";
            loading.hidden = false;
            error.hidden = true;
            zoomButtons.forEach(button => button.hidden = type !== "image");
            modal.classList.add("is-open");

            if (!url || type === "unsupported") {
                loading.hidden = true;
                error.hidden = false;
                return;
            }

            if (type === "pdf") {
                const iframe = document.createElement("iframe");
                iframe.title = title.textContent;
                iframe.src = url;
                iframe.addEventListener("load", () => { loading.hidden = true; });
                stage.appendChild(iframe);
                window.setTimeout(() => { loading.hidden = true; }, 1200);
                return;
            }

            const image = document.createElement("img");
            image.alt = title.textContent;
            image.onload = function () {
                loading.hidden = true;
            };
            image.onerror = function () {
                loading.hidden = true;
                error.hidden = false;
                stage.innerHTML = "";
            };
            image.src = url;
            stage.appendChild(image);
        }

        function closePreview() {
            const modal = document.getElementById("studentDocumentPreviewModal");
            if (!modal) return;
            modal.classList.remove("is-open");
            modal.querySelector(".doc-preview-stage").innerHTML = "";
        }

        function downloadDoc(doc) {
            const url = docUrl(doc);
            if (!url) return;
            const link = document.createElement("a");
            link.href = url;
            link.download = doc.fileName || doc.name || "document";
            link.target = "_blank";
            document.body.appendChild(link);
            link.click();
            link.remove();
        }

        function makeIconButton(label, color, title) {
            const button = document.createElement("button");
            button.type = "button";
            button.textContent = label;
            button.style.border = "0";
            button.style.background = "transparent";
            button.style.color = color;
            button.style.fontSize = "13px";
            button.style.cursor = "pointer";
            button.style.fontWeight = "700";
            button.title = title || label;
            return button;
        }

        function render() {
            if (!listBody) return;
            listBody.innerHTML = "";
            syncState();
            if (!docs.length) {
                const row = document.createElement("tr");
                row.setAttribute("data-doc-empty", "true");
                const td = document.createElement("td");
                td.colSpan = 4;
                td.style.height = "34px";
                td.style.border = "1px solid #e2e8f0";
                td.style.borderLeft = "0";
                td.style.color = "#94a3b8";
                td.style.padding = "8px 10px";
                td.innerHTML = "&nbsp;";
                row.appendChild(td);
                listBody.appendChild(row);
                return;
            }

            docs.forEach((doc, index) => {
                const row = document.createElement("tr");
                const nameCell = document.createElement("td");
                nameCell.style.padding = "7px 8px";
                nameCell.style.border = "1px solid #e2e8f0";
                nameCell.style.borderLeft = "0";
                nameCell.style.color = "#335a97";
                nameCell.textContent = doc.name;

                const downloadCell = document.createElement("td");
                downloadCell.style.padding = "7px 8px";
                downloadCell.style.border = "1px solid #e2e8f0";
                downloadCell.style.textAlign = "center";
                const downloadBtn = makeIconButton("⬇", "#0f9d3c", doc.fileName ? `${doc.fileName} ${humanSize(doc.fileSize)}` : "Download");
                downloadBtn.addEventListener("click", () => downloadDoc(doc));
                downloadCell.appendChild(downloadBtn);

                const viewCell = document.createElement("td");
                viewCell.style.padding = "7px 8px";
                viewCell.style.border = "1px solid #e2e8f0";
                viewCell.style.textAlign = "center";
                const viewBtn = makeIconButton("👁 View", "#1d4ed8", "View document");
                viewBtn.addEventListener("click", () => openPreview(doc));
                viewCell.appendChild(viewBtn);

                const deleteCell = document.createElement("td");
                deleteCell.style.padding = "7px 8px";
                deleteCell.style.border = "1px solid #e2e8f0";
                deleteCell.style.textAlign = "center";
                const deleteBtn = makeIconButton("🗑", doc.existing ? "#94a3b8" : "#ff3b30", doc.existing ? "Saved documents can be replaced by uploading the same document name" : "Delete");
                deleteBtn.disabled = Boolean(doc.existing);
                deleteBtn.style.cursor = doc.existing ? "not-allowed" : "pointer";
                deleteBtn.addEventListener("click", function () {
                    if (doc.existing) return;
                    removeHiddenInputs(doc);
                    docs.splice(index, 1);
                    render();
                });
                deleteCell.appendChild(deleteBtn);

                row.appendChild(nameCell);
                row.appendChild(downloadCell);
                row.appendChild(viewCell);
                row.appendChild(deleteCell);
                listBody.appendChild(row);
            });
        }

        function clearValidation() {
            setInvalid(nameField, false);
            setInvalid(fileField, false);
        }

        function addDocument() {
            clearValidation();
            const name = (nameField && nameField.value || "").trim();
            const file = fileField && fileField.files ? fileField.files[0] : null;
            if (!name) {
                setInvalid(nameField, true);
                nameField && nameField.focus();
                return;
            }
            if (!file) {
                setInvalid(fileField, true);
                fileField && fileField.focus();
                return;
            }
            if (file.size > maxSize) {
                setInvalid(fileField, true);
                fileField.focus();
                return;
            }
            const allowed = /\.(jpg|jpeg|png|gif|pdf)$/i.test(file.name);
            if (!allowed) {
                setInvalid(fileField, true);
                fileField.focus();
                return;
            }
            const existingIndex = docs.findIndex(function (doc) {
                return doc.name && doc.name.toLowerCase() === name.toLowerCase();
            });
            if (existingIndex >= 0) {
                removeHiddenInputs(docs[existingIndex]);
                docs.splice(existingIndex, 1);
            }
            docs.push({
                name: name,
                fileName: file.name,
                fileSize: file.size,
                file: file,
                token: `doc-${Date.now()}-${docCounter++}`,
                status: "PENDING",
                existing: false
            });
            attachHiddenInputs(docs[docs.length - 1]);
            syncState();
            if (nameField) nameField.value = "";
            if (fileField) fileField.value = "";
            render();
        }

        if (listBody) {
            listBody.querySelectorAll("[data-existing-doc]").forEach(function (row) {
                if (!row.dataset.url) return;
                docs.push({
                    name: row.dataset.name || "Document",
                    fileName: row.dataset.file || row.dataset.url,
                    fileSize: row.dataset.size || "",
                    url: row.dataset.url,
                    token: `existing-${docCounter++}`,
                    existing: true
                });
            });
        }

        if (addButton) {
            addButton.addEventListener("click", addDocument);
        }

        if (nameField) {
            nameField.addEventListener("input", clearValidation);
            nameField.addEventListener("change", clearValidation);
        }
        if (fileField) {
            fileField.addEventListener("change", clearValidation);
        }

        document.addEventListener("click", function (event) {
            const closeButton = event.target.closest("[data-doc-preview-close]");
            if (closeButton) {
                closePreview();
                return;
            }
            const zoomButton = event.target.closest("[data-doc-preview-zoom]");
            if (zoomButton) {
                const image = document.querySelector("#studentDocumentPreviewModal .doc-preview-stage img");
                if (!image) return;
                previewZoom += zoomButton.dataset.docPreviewZoom === "in" ? 0.15 : -0.15;
                previewZoom = Math.min(2.5, Math.max(0.5, previewZoom));
                image.style.transform = `scale(${previewZoom})`;
            }
        });

        document.addEventListener("keydown", function (event) {
            if (event.key === "Escape") closePreview();
        });

        if (form) {
            form.addEventListener("submit", function () {
                if (form.dataset.docList) {
                    const hidden = form.querySelector('input[name="documentUploadsJson"]');
                    if (!hidden) {
                        const input = document.createElement("input");
                        input.type = "hidden";
                        input.name = "documentUploadsJson";
                        input.value = form.dataset.docList;
                        form.appendChild(input);
                    } else {
                        hidden.value = form.dataset.docList;
                    }
                }
            });
        }

        syncState();
        render();
    });
})();
