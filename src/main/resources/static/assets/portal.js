"use strict";

const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
const page = document.body.dataset.page;
const role = document.body.dataset.role;
const state = {
    warehouseId: localStorage.getItem("wpo.activeWarehouseId") || "",
    warehouses: [],
    warehouse: null,
    articles: [],
    containers: [],
    places: [],
    assessment: null,
    plan: null,
    recommendation: null
};
const rackLevelDrafts = [];

const labels = {
    ACTIVE: "Active", INACTIVE: "Inactive", AVAILABLE: "Available", OCCUPIED: "Occupied",
    WAITING_FOR_PLACEMENT: "Waiting for placement", STORED: "Stored", MERGED: "Merged", REMOVED: "Removed",
    SUGGESTED: "Suggested", ACCEPTED: "Accepted", REJECTED: "Rejected", EXPIRED: "Expired",
    HEALTHY: "Healthy", OPTIMIZATION_RECOMMENDED: "Optimization recommended", INSUFFICIENT_DATA: "Insufficient data",
    DRAFT: "Draft", APPROVED: "Approved", IN_PROGRESS: "In progress", COMPLETED: "Completed",
    CANCELLED: "Cancelled", PENDING: "Pending", READY: "Ready", TRAINING: "Training", SUPERSEDED: "Superseded",
    ROOT_ADMIN: "Root administrator", ADMIN: "Administrator", OPERATOR: "Operator",
    PCS: "Pieces", BOX: "Boxes", PLACE: "Place", MOVE: "Move", TEMPORARY_MOVE: "Temporary move",
    PUTAWAY: "Putaway", RELOCATION: "Relocation", TEMPORARY_RELOCATION: "Temporary relocation",
    REMOVAL: "Removal", TRIBUO: "Machine learning", BASELINE: "Baseline", SEASONAL: "Seasonal forecast"
};

class ApiError extends Error {
    constructor(status, payload) {
        super(payload?.message || `HTTP ${status}`);
        this.status = status;
        this.payload = payload;
    }
}

async function api(path, options = {}) {
    const headers = new Headers(options.headers || {});
    if (options.body !== undefined) headers.set("Content-Type", "application/json");
    const response = await fetch(path, {
        method: options.method || "GET",
        headers,
        credentials: "same-origin",
        body: options.body === undefined ? undefined : JSON.stringify(options.body)
    });
    const contentType = response.headers.get("content-type") || "";
    const responseText = response.status === 204 ? "" : await response.text();
    let payload = responseText || null;
    if (responseText && contentType.includes("json")) {
        try {
            payload = JSON.parse(responseText);
        } catch {
            throw new Error(`The server returned invalid JSON for ${path}`);
        }
    }
    if (response.status === 401) {
        location.assign("/login?expired");
        throw new ApiError(response.status, payload);
    }
    if (!response.ok) throw new ApiError(response.status, payload);
    return payload;
}

function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>'"]/g, char => ({"&":"&amp;","<":"&lt;",">":"&gt;","'":"&#39;",'"':"&quot;"}[char]));
}

function label(value) {
    return labels[value] || value || "—";
}

function badge(value) {
    const good = ["ACTIVE", "AVAILABLE", "STORED", "ACCEPTED", "APPROVED", "HEALTHY", "COMPLETED"];
    const warn = ["WAITING_FOR_PLACEMENT", "SUGGESTED", "DRAFT", "READY", "IN_PROGRESS", "OPTIMIZATION_RECOMMENDED", "TRAINING", "PENDING"];
    const bad = ["INACTIVE", "REMOVED", "REJECTED", "CANCELLED", "EXPIRED", "FAILED"];
    const tone = good.includes(value) ? "good" : warn.includes(value) ? "warn" : bad.includes(value) ? "bad" : "info";
    return `<span class="badge ${tone}">${escapeHtml(label(value))}</span>`;
}

function formatDate(value) {
    if (!value) return "—";
    return new Intl.DateTimeFormat("en-GB", {dateStyle: "medium", timeStyle: "short"}).format(new Date(value));
}

function num(value) {
    return value === "" || value == null ? null : Number(value);
}

function formData(form) {
    return Object.fromEntries(new FormData(form).entries());
}

function toast(message, type = "success") {
    const root = $("#toastStack");
    if (!root) return;
    const item = document.createElement("div");
    item.className = `toast ${type}`;
    item.textContent = message;
    root.append(item);
    setTimeout(() => item.remove(), 4200);
}

function report(error) {
    console.error(error);
    const payloadMessage = typeof error.payload === "string" ? error.payload : error.payload?.message;
    toast(payloadMessage || error.message || "The action could not be completed", "error");
}

function requireWarehouse() {
    if (!state.warehouseId) throw new Error("Select an active warehouse first");
    return state.warehouseId;
}

function setProcessStep(selector, activeIndex) {
    const steps = $$(`${selector} .process-step`);
    steps.forEach((step, index) => {
        step.classList.toggle("active", index === activeIndex);
        step.classList.toggle("done", index < activeIndex || activeIndex >= steps.length);
    });
}

function bindShell() {
    $("#menuButton")?.addEventListener("click", () => $("#sidebar")?.classList.toggle("open"));
    $("[data-refresh]")?.addEventListener("click", refreshFromToolbar);
    $$('[data-dialog-open]').forEach(button => button.addEventListener("click", () => {
        const dialogId = button.dataset.dialogOpen;
        const dialog = $(`#${dialogId}`);
        if (dialogId === "articleDialog") {
            const form = $("#articleForm");
            form.reset();
            form.elements.id.value = "";
            form.elements.articleNumber.disabled = false;
            $("#articleDialogTitle").textContent = "New article";
        }
        if (dialogId === "accountDialog") {
            const form = $("#accountForm");
            form.reset();
            form.elements.id.value = "";
            form.elements.password.required = true;
            delete form.dataset.original;
            $("#accountDialogTitle").textContent = "New user";
        }
        if (dialogId === "warehouseDialog") {
            const form = $("#warehouseForm");
            form.reset();
            rackLevelDrafts.length = 0;
            $("#levelProfiles").innerHTML = "";
            renderRackLevelProfiles(num(form.elements.rackLevelCount.value));
        }
        dialog?.showModal();
    }));
    $$('[data-dialog-close]').forEach(button => button.addEventListener("click", () => button.closest("dialog")?.close()));
    $$(".tab").forEach(tab => tab.addEventListener("click", () => {
        $$(".tab").forEach(item => item.classList.toggle("active", item === tab));
        $$(".tab-panel").forEach(panel => panel.classList.toggle("active", panel.id === `tab-${tab.dataset.tab}`));
    }));
}

async function loadWarehouses(preferredWarehouseId = state.warehouseId) {
    const picker = $("#warehouseSelect");
    if (!picker) return;
    state.warehouses = await api(role === "operator" ? "/operator/warehouses" : "/admin/warehouses");
    state.warehouseId = preferredWarehouseId ? String(preferredWarehouseId) : state.warehouseId;
    if (!state.warehouseId && state.warehouses.length) state.warehouseId = String(state.warehouses[0].id);
    if (state.warehouseId && !state.warehouses.some(item => String(item.id) === String(state.warehouseId))) {
        state.warehouseId = state.warehouses.length ? String(state.warehouses[0].id) : "";
    }
    picker.innerHTML = state.warehouses.length
        ? state.warehouses.map(item => `<option value="${item.id}">${escapeHtml(item.warehouseCode)} · ${escapeHtml(item.warehouseName)}</option>`).join("")
        : '<option value="">No warehouses found</option>';
    picker.value = state.warehouseId;
    localStorage.setItem("wpo.activeWarehouseId", state.warehouseId);
    picker.onchange = async event => {
        state.warehouseId = event.target.value;
        localStorage.setItem("wpo.activeWarehouseId", state.warehouseId);
        try {
            await refreshCurrentPage();
            toast("Active warehouse changed");
        } catch (error) {
            report(error);
        }
    };
}

async function refreshFromToolbar(event) {
    const button = event.currentTarget;
    const originalText = button.textContent;
    button.disabled = true;
    button.textContent = "Refreshing...";
    try {
        await loadWarehouses(state.warehouseId);
        await refreshCurrentPage();
        toast("Data refreshed");
    } catch (error) {
        report(error);
    } finally {
        button.disabled = false;
        button.textContent = originalText;
    }
}

async function loadAdminDashboard() {
    const warehouseId = requireWarehouse();
    const results = await Promise.allSettled([
        api(`/admin/warehouses/${warehouseId}`), api(`/admin/warehouses/${warehouseId}/storage-places`),
        api("/operator/containers"), api("/admin/articles"),
        api(`/admin/warehouses/${warehouseId}/optimization-assessments/latest`),
        api(`/admin/warehouses/${warehouseId}/demand-forecast-models/latest`)
    ]);
    const value = index => results[index].status === "fulfilled" ? results[index].value : null;
    const warehouse = value(0), places = value(1) || [], containers = (value(2) || []).filter(item => String(item.warehouseId) === String(warehouseId));
    const articles = value(3) || [], assessment = value(4), model = value(5);
    $("#heroWarehouseName").textContent = warehouse?.warehouseName || "Select a warehouse";
    $("#metricPlaces").textContent = places.length;
    $("#metricAvailable").textContent = `${places.filter(item => item.status === "AVAILABLE").length} available`;
    $("#metricContainers").textContent = containers.length;
    $("#metricWaiting").textContent = `${containers.filter(item => item.status === "WAITING_FOR_PLACEMENT").length} waiting`;
    $("#metricArticles").textContent = articles.length;
    if (model) { $("#metricModel").textContent = `v${model.versionNumber}`; $("#metricModelDate").textContent = formatDate(model.trainedAt); }
    if (assessment) {
        const score = Number(assessment.scorePercent || 0);
        $("#dashboardScore").style.setProperty("--score", score);
        $("#scoreValue").textContent = `${score.toFixed(1)}%`;
    }
    const waiting = containers.filter(item => item.status === "WAITING_FOR_PLACEMENT").length;
    const attention = [
        {title: `${waiting} pallets waiting for placement`, text: waiting ? "Operators need to complete receiving." : "The placement queue is empty.", href: "/app/operator/placement"},
        {title: assessment?.optimizationRecommended ? "The warehouse needs optimization" : "No critical relocations are required", text: assessment ? `Latest assessment: ${assessment.scorePercent}%` : "No assessment has been run yet.", href: "/app/admin/optimization"},
        {title: model ? `Active demand model v${model.versionNumber}` : "Demand model not trained yet", text: model ? formatDate(model.trainedAt) : "Import order history.", href: "/app/admin/demand"}
    ];
    $("#adminAttention").classList.remove("loading-state");
    $("#adminAttention").innerHTML = attention.map(item => `<a class="task-item" href="${item.href}"><div><b>${escapeHtml(item.title)}</b><p>${escapeHtml(item.text)}</p></div><span>→</span></a>`).join("");
}

async function loadWarehousePage() {
    renderRackLevelProfiles(num($("#warehouseForm").elements.rackLevelCount.value));
    $("#warehouseForm").elements.rackLevelCount.addEventListener("input", event => renderRackLevelProfiles(num(event.target.value)));
    $("#warehouseForm").elements.palletPlacesPerLevel.addEventListener("input", updateBayLoadSummary);
    $("#warehouseForm").elements.maxBayLoadKg.addEventListener("input", updateBayLoadSummary);
    $("[data-load-places]").addEventListener("click", () => loadPlaces().catch(report));
    $("#placeSearch").addEventListener("input", renderPlaces);
    $("#placeStatus").addEventListener("change", () => loadPlaces().catch(report));
    $("#routeForm").addEventListener("submit", event => { event.preventDefault(); loadRoute(formData(event.currentTarget).storagePlaceCode).catch(report); });
    $("#warehouseForm").addEventListener("submit", createWarehouse);
    await refreshWarehouseData();
}

async function refreshWarehouseData() {
    if (!state.warehouseId) {
        state.warehouse = null;
        state.places = [];
        $("#warehouseAisles").textContent = "—";
        $("#warehouseRows").textContent = "—";
        $("#warehousePlaces").textContent = "—";
        $("#warehouseStatus").textContent = "No warehouse";
        renderPlaces();
        return;
    }
    state.warehouse = await api(`/admin/warehouses/${requireWarehouse()}`);
    $("#warehouseAisles").textContent = state.warehouse.aisleCount ?? "—";
    $("#warehouseRows").textContent = state.warehouse.rackRowCount ?? "—";
    $("#warehousePlaces").textContent = state.warehouse.storagePlaceCount ?? "—";
    $("#warehouseStatus").innerHTML = badge(state.warehouse.status);
    await loadPlaces();
}

function captureRackLevelDrafts() {
    $$('[data-level-profile]').forEach(card => {
        const index = Number(card.dataset.levelProfile) - 1;
        rackLevelDrafts[index] = {
            clearHeightMm: card.querySelector('[data-level-height]').value,
            maxCellLoadKg: card.querySelector('[data-level-weight]').value
        };
    });
}

function renderRackLevelProfiles(levelCount) {
    const root = $("#levelProfiles");
    if (!root) return;
    captureRackLevelDrafts();
    const safeCount = Math.min(20, Math.max(1, Number.isFinite(levelCount) ? levelCount : 1));
    root.innerHTML = Array.from({length: safeCount}, (_, index) => {
        const level = index + 1;
        const draft = rackLevelDrafts[index] || {
            clearHeightMm: Math.max(800, 2000 - level * 200),
            maxCellLoadKg: Math.max(500, 1100 - level * 100)
        };
        rackLevelDrafts[index] = draft;
        return `<div data-level-profile="${level}"><b>Level ${level}</b><label>Height, mm<input data-level-height type="number" min="1" value="${escapeHtml(draft.clearHeightMm)}" required></label><label>Weight, kg<input data-level-weight type="number" min="1" value="${escapeHtml(draft.maxCellLoadKg)}" required></label></div>`;
    }).join("");
    $$('[data-level-profile] input', root).forEach(input => input.addEventListener("input", updateBayLoadSummary));
    updateBayLoadSummary();
}

function calculatedBayLoad() {
    const places = num($("#warehouseForm")?.elements.palletPlacesPerLevel.value) || 0;
    const levelLoad = $$('[data-level-weight]').reduce((total, input) => total + (num(input.value) || 0), 0);
    return places * levelLoad;
}

function updateBayLoadSummary() {
    const summary = $("#bayLoadSummary");
    if (!summary) return;
    const calculated = calculatedBayLoad();
    const limit = num($("#warehouseForm").elements.maxBayLoadKg.value) || 0;
    const exceeds = calculated > limit;
    summary.textContent = `Calculated bay load: ${calculated.toLocaleString("en-GB")} kg${exceeds ? " (exceeds limit)" : ""}`;
    summary.classList.toggle("validation-error", exceeds);
}

async function loadPlaces() {
    const status = $("#placeStatus")?.value || "";
    state.places = await api(`/admin/warehouses/${requireWarehouse()}/storage-places${status ? `?status=${status}` : ""}`);
    renderPlaces();
}

function renderPlaces() {
    const query = $("#placeSearch")?.value.trim().toLowerCase() || "";
    const items = state.places.filter(item => !query || item.code.toLowerCase().includes(query));
    $("#placesTable").innerHTML = items.length ? items.map(item => `<tr><td><b>${escapeHtml(item.code)}</b></td><td>${escapeHtml(item.rackRowCode)} / L${item.levelNumber}</td><td>${(item.distanceFromEntryMm / 1000).toFixed(1)} m</td><td>${item.maxWeightKg} kg · ${item.maxHeightMm} mm</td><td>${badge(item.status)}</td><td><button class="mini-button" data-route="${escapeHtml(item.code)}">Route</button></td></tr>`).join("") : '<tr><td colspan="6" class="empty-cell">No locations found</td></tr>';
}

async function loadRoute(code) {
    const route = await api(`/admin/warehouses/${requireWarehouse()}/routes/storage-places/${encodeURIComponent(code)}`);
    $("#routeForm [name=storagePlaceCode]").value = code;
    $("#routeSummary").innerHTML = `<b>${escapeHtml(route.storagePlaceCode)}</b> · ${(route.distanceMm / 1000).toFixed(1)} m · approximately ${route.estimatedTravelTimeSeconds} sec`;
    const svg = $("#routeMap");
    if (!route.points?.length) { svg.innerHTML = ""; return; }
    const xs = route.points.map(point => point.xMm), ys = route.points.map(point => point.yMm);
    const minX = Math.min(...xs), maxX = Math.max(...xs), minY = Math.min(...ys), maxY = Math.max(...ys);
    const scaleX = x => 35 + (x - minX) / Math.max(maxX - minX, 1) * 650;
    const scaleY = y => 35 + (y - minY) / Math.max(maxY - minY, 1) * 190;
    const points = route.points.map(point => `${scaleX(point.xMm)},${scaleY(point.yMm)}`).join(" ");
    svg.innerHTML = `<polyline points="${points}" fill="none" stroke="#17231e" stroke-width="7" stroke-linecap="round" stroke-linejoin="round"/>${route.points.map((point, index) => `<circle cx="${scaleX(point.xMm)}" cy="${scaleY(point.yMm)}" r="${index === route.points.length - 1 ? 10 : 6}" fill="${index === route.points.length - 1 ? "#d9ff63" : "#57d39b"}" stroke="#17231e" stroke-width="3"/>`).join("")}`;
}

async function createWarehouse(event) {
    event.preventDefault();
    const data = formData(event.currentTarget);
    const levelProfiles = $$('[data-level-profile]').map(card => ({
        levelNumber: num(card.dataset.levelProfile),
        clearHeightMm: num(card.querySelector('[data-level-height]').value),
        maxCellLoadKg: num(card.querySelector('[data-level-weight]').value)
    }));
    delete data.rackLevelCount;
    if (calculatedBayLoad() > num(data.maxBayLoadKg)) {
        toast("Calculated rack load exceeds the bay load limit", "error");
        return;
    }
    let created;
    try {
        created = await api("/admin/warehouses", {method: "POST", body: {...data, aisleCount:num(data.aisleCount), rackRowCount:num(data.rackRowCount), baysPerRackRow:num(data.baysPerRackRow), palletPlacesPerLevel:num(data.palletPlacesPerLevel), aisleWidthMm:num(data.aisleWidthMm), maxBayLoadKg:num(data.maxBayLoadKg), levelProfiles}});
    } catch (error) {
        report(error);
        return;
    }
    $("#warehouseDialog").close();
    event.currentTarget.reset();
    state.warehouseId = String(created.id);
    localStorage.setItem("wpo.activeWarehouseId", state.warehouseId);
    toast("Warehouse created");
    try {
        await loadWarehouses(state.warehouseId);
        await refreshWarehouseData();
    } catch (error) {
        console.error(error);
        toast("Warehouse was created, but the page data could not be refreshed", "error");
    }
}

async function loadArticles() {
    state.articles = await api("/admin/articles");
    $("#articlesTable").innerHTML = state.articles.length ? state.articles.map(item => `<tr><td><b>${escapeHtml(item.articleNumber)}</b></td><td>${escapeHtml(item.name)}</td><td>${escapeHtml(label(item.unitType))}</td><td>${item.unitWidthMm}×${item.unitLengthMm}×${item.unitHeightMm} mm</td><td>${item.unitWeightKg} kg</td><td>${item.maxQuantityPerPallet}</td><td><div class="row-actions"><button class="mini-button" data-article-edit="${item.id}">Edit</button><button class="mini-button danger" data-article-delete="${item.id}">Delete</button></div></td></tr>`).join("") : '<tr><td colspan="7" class="empty-cell">No articles found</td></tr>';
}

async function loadContainers() {
    state.containers = await api("/operator/containers");
    renderContainers();
}

function renderContainers() {
    const query = $("#containerSearch")?.value.trim().toLowerCase() || "";
    const status = $("#containerStatus")?.value || "";
    const items = state.containers.filter(item => (!state.warehouseId || String(item.warehouseId) === String(state.warehouseId)) && (!status || item.status === status) && (!query || `${item.containerNumber} ${item.articleNumber} ${item.articleName}`.toLowerCase().includes(query)));
    const actions = page === "operator-inventory";
    $("#containersTable").innerHTML = items.length ? items.map(item => `<tr><td><b>${escapeHtml(item.containerNumber)}</b></td><td>${escapeHtml(item.articleNumber)}<small class="block">${escapeHtml(item.articleName)}</small></td><td>${item.quantity}</td><td>${item.weightKg} kg · ${item.heightMm} mm</td><td>${escapeHtml(item.currentStoragePlaceCode || "—")}</td><td>${badge(item.status)}</td>${actions ? `<td><div class="row-actions"><button class="mini-button" data-container-edit="${escapeHtml(item.containerNumber)}">Edit</button><button class="mini-button danger" data-container-remove="${escapeHtml(item.containerNumber)}">Remove</button></div></td>` : ""}</tr>`).join("") : `<tr><td colspan="${actions ? 7 : 6}" class="empty-cell">No pallets found</td></tr>`;
}

async function loadAdminInventory() {
    await Promise.all([loadArticles(), loadContainers()]);
    $("#containerSearch").addEventListener("input", renderContainers);
    $("[data-load-containers]").addEventListener("click", () => loadContainers().catch(report));
    $("#articleForm").addEventListener("submit", saveArticle);
}

async function saveArticle(event) {
    event.preventDefault(); const data = formData(event.currentTarget), id = data.id; delete data.id;
    if (id) delete data.articleNumber;
    ["unitWidthMm", "unitLengthMm", "unitHeightMm", "unitWeightKg", "maxQuantityPerPallet"].forEach(key => data[key] = num(data[key]));
    try {
        await api(id ? `/admin/articles/${id}` : "/admin/articles", {method: id ? "PATCH" : "POST", body: data});
    } catch (error) {
        report(error);
        return;
    }
    $("#articleDialog").close();
    event.currentTarget.reset();
    toast("Article saved");
    try {
        await loadArticles();
    } catch (error) {
        console.error(error);
        toast("Article was saved, but the list could not be refreshed", "error");
    }
}

function renderModel(model) {
    $("#modelVersion").textContent = `v${model.versionNumber} · ${label(model.status)}`;
    $("#modelAlgorithm").textContent = model.algorithm?.toLowerCase().includes("tribuo") ? "Tribuo ML" : model.algorithm || "—";
    $("#modelImprovement").textContent = model.improvementPercent == null ? "—" : `${model.improvementPercent.toFixed(1)}%`;
    $("#modelObservations").textContent = model.observationCount;
}

async function loadDemand() {
    const data = formData($("#demandFilterForm")); const params = new URLSearchParams();
    if (data.from) params.set("from", data.from); if (data.to) params.set("to", data.to);
    const items = await api(`/admin/warehouses/${requireWarehouse()}/demand/articles${params.size ? `?${params}` : ""}`);
    $("#demandTable").innerHTML = items.length ? items.slice(0, Number(data.limit || 30)).map(item => `<tr><td>#${item.rank}</td><td><b>${escapeHtml(item.articleNumber)}</b><small class="block">${escapeHtml(item.articleName)}</small></td><td>${item.demandScore.toFixed(3)}</td><td>${item.historicalQuantity} / ${item.orderCount} orders</td><td>${item.storedQuantity} · ${item.storedContainerCount} pallets</td><td>${item.averageDistanceFromEntryMm ? `${(item.averageDistanceFromEntryMm / 1000).toFixed(1)} m` : "—"}</td><td>${badge(item.scoreSource)}</td></tr>`).join("") : '<tr><td colspan="7" class="empty-cell">No demand data found</td></tr>';
}

async function loadModels() {
    const items = await api(`/admin/warehouses/${requireWarehouse()}/demand-forecast-models`);
    $("#modelsTable").innerHTML = items.length ? items.map(item => `<tr><td><b>v${item.versionNumber}</b><small class="block">${escapeHtml(item.code)}</small></td><td>${badge(item.status)}</td><td>${item.trainingStart} → ${item.trainingEnd}</td><td>${item.modelMae?.toFixed(3) ?? "—"}</td><td>${item.baselineMae?.toFixed(3) ?? "—"}</td><td>${item.improvementPercent?.toFixed(1) ?? "—"}%</td><td>${formatDate(item.trainedAt)}</td></tr>`).join("") : '<tr><td colspan="7" class="empty-cell">No models found</td></tr>';
}

async function loadAdminDemand() {
    await refreshAdminDemandData();
    $("#demandFilterForm").addEventListener("submit", event => { event.preventDefault(); loadDemand().catch(report); });
    $("#trainModelButton").addEventListener("click", async () => { try { const model = await api(`/admin/warehouses/${requireWarehouse()}/demand-forecast-models/train`, {method:"POST"}); renderModel(model); await loadModels(); toast("Model trained"); } catch (error) { report(error); } });
    $("#demandImportForm").addEventListener("submit", async event => { event.preventDefault(); try { const result = await api("/admin/demand-history/import", {method:"POST", body:{warehouseId:num(requireWarehouse()), orders:JSON.parse(formData(event.currentTarget).orders)}}); $("#demandDialog").close(); await loadDemand(); toast(`Imported orders: ${result.importedOrders}`); } catch (error) { report(error); } });
}

async function refreshAdminDemandData() {
    const results = await Promise.allSettled([loadDemand(), api(`/admin/warehouses/${requireWarehouse()}/demand-forecast-models/latest`), loadModels()]);
    if (results[1].status === "fulfilled") renderModel(results[1].value);
    const firstFailure = results.find(result => result.status === "rejected" && result.reason?.status !== 404);
    if (firstFailure) throw firstFailure.reason;
}

function renderAssessment(item) {
    state.assessment = item; const score = Number(item.scorePercent || 0);
    $("#optimizationRing").style.setProperty("--score", score); $("#optimizationScore").textContent = `${score.toFixed(1)}%`;
    $("#optimizationHeadline").textContent = item.optimizationRecommended ? "A relocation plan is required" : item.status === "INSUFFICIENT_DATA" ? "Insufficient data" : "The warehouse is sufficiently optimized";
    $("#optimizationDetails").textContent = `Threshold ${item.thresholdPercent}%. Analyzed ${item.analyzedContainerCount} pallets and ${item.demandObservationCount} demand observations.`;
    $("#assessmentDetails").innerHTML = `<div><dt>Status</dt><dd>${label(item.status)}</dd></div><div><dt>Pallets with demand</dt><dd>${item.demandMatchedContainerCount}/${item.analyzedContainerCount}</dd></div><div><dt>Average distance</dt><dd>${item.weightedAverageDistanceMm ? `${(item.weightedAverageDistanceMm / 1000).toFixed(1)} m` : "—"}</dd></div><div><dt>Assessed at</dt><dd>${formatDate(item.analyzedAt)}</dd></div>`;
    $("#createPlanButton").disabled = !item.optimizationRecommended;
    setProcessStep(".optimization-steps", item.optimizationRecommended ? 1 : 0);
}

function renderPlan(plan) {
    state.plan = plan;
    $("#planQuickState").innerHTML = `<b>${escapeHtml(plan.code)}</b> · ${label(plan.status)} · ${plan.completedSteps}/${plan.totalSteps} steps`;
    $("#planCard").innerHTML = `<div class="section-head"><div><p class="eyebrow">${escapeHtml(plan.code)}</p><h2>Relocation plan</h2></div>${badge(plan.status)}</div><div class="metric-grid compact"><article class="metric"><span>Current</span><b>${plan.initialScorePercent}%</b></article><article class="metric"><span>Projected</span><b>${plan.projectedScorePercent}%</b></article><article class="metric"><span>Target</span><b>${plan.targetScorePercent}%</b></article><article class="metric"><span>Time saved</span><b>${Math.round(plan.estimatedTimeSavingSeconds / 60)} min</b></article></div><div class="button-row"><button class="button primary" data-plan-approve="${escapeHtml(plan.code)}" ${plan.status !== "DRAFT" ? "disabled" : ""}>Approve for operator</button><button class="button ghost" data-plan-cancel="${escapeHtml(plan.code)}" ${["COMPLETED","CANCELLED"].includes(plan.status) ? "disabled" : ""}>Cancel</button><a class="button subtle" href="/app/operator/relocation?plan=${encodeURIComponent(plan.code)}">Open execution</a></div><div class="step-list">${(plan.steps || []).map(step => `<div class="step-item"><span class="step-number">${step.sequenceNumber}</span><div><b>${escapeHtml(label(step.type))} · ${escapeHtml(step.sourceContainerNumber)}</b><small>${escapeHtml(step.fromStoragePlaceCode || "—")} → ${escapeHtml(step.toStoragePlaceCode || step.targetContainerNumber || "—")}</small></div>${badge(step.status)}</div>`).join("")}</div>`;
    setProcessStep(".optimization-steps", plan.status === "DRAFT" ? 2 : ["APPROVED","IN_PROGRESS"].includes(plan.status) ? 3 : plan.status === "COMPLETED" ? 4 : 0);
}

async function loadAdminOptimization() {
    await refreshAdminOptimizationData();
    $("#runAssessmentButton").addEventListener("click", async () => { try { renderAssessment(await api(`/admin/warehouses/${requireWarehouse()}/optimization-assessments`, {method:"POST"})); toast("Assessment completed"); } catch (error) { report(error); } });
    $("#latestAssessmentButton").addEventListener("click", async () => { try { renderAssessment(await api(`/admin/warehouses/${requireWarehouse()}/optimization-assessments/latest`)); } catch (error) { report(error); } });
    $("#createPlanButton").addEventListener("click", async () => { try { renderPlan(await api(`/admin/optimization-plans/assessments/${state.assessment.id}`, {method:"POST"})); toast("Plan created"); } catch (error) { report(error); } });
    $("#planLookupForm").addEventListener("submit", async event => { event.preventDefault(); try { renderPlan(await api(`/admin/optimization-plans/${encodeURIComponent(formData(event.currentTarget).planCode)}`)); } catch (error) { report(error); } });
}

async function refreshAdminOptimizationData() {
    try {
        renderAssessment(await api(`/admin/warehouses/${requireWarehouse()}/optimization-assessments/latest`));
    } catch (error) {
        if (error.status !== 404) throw error;
    }
}

async function loadAccounts() {
    const items = await api("/admin/accounts");
    $("#accountsTable").innerHTML = items.length ? items.map(item => {
        const actions = item.role === "ROOT_ADMIN"
            ? '<span class="badge info">Protected account</span>'
            : `<div class="row-actions"><button class="mini-button" data-account-edit="${item.id}">Edit</button><button class="mini-button" data-account-toggle="${item.id}" data-status="${item.status}">${item.status === "ACTIVE" ? "Deactivate" : "Activate"}</button></div>`;
        return `<tr><td><b>${escapeHtml(item.fullName)}</b><small class="block">${escapeHtml(item.email)}</small></td><td>${badge(item.role)}</td><td>${badge(item.status)}</td><td>${formatDate(item.createdAt)}</td><td>${formatDate(item.updatedAt)}</td><td>${actions}</td></tr>`;
    }).join("") : '<tr><td colspan="6" class="empty-cell">No users found</td></tr>';
}

async function loadAdminAccounts() {
    await loadAccounts();
    $("#accountForm").addEventListener("submit", saveAccount);
}

async function saveAccount(event) {
    event.preventDefault();
    const data = formData(event.currentTarget), id = data.id;
    delete data.id;
    if (id) {
        const original = JSON.parse(event.currentTarget.dataset.original || "{}");
        ["email", "fullName", "role"].forEach(key => {
            if (data[key] === String(original[key] ?? "")) delete data[key];
        });
        if (!data.password) delete data.password;
        if (!Object.keys(data).length) {
            $("#accountDialog").close();
            toast("No account changes to save");
            return;
        }
    }
    try {
        await api(id ? `/admin/accounts/${id}` : "/admin/accounts", {method:id ? "PATCH" : "POST", body:data});
    } catch (error) {
        report(error);
        return;
    }
    $("#accountDialog").close();
    event.currentTarget.reset();
    delete event.currentTarget.dataset.original;
    toast("User saved");
    try {
        await loadAccounts();
    } catch (error) {
        console.error(error);
        toast("User was saved, but the list could not be refreshed", "error");
    }
}

async function loadOperatorDashboard() {
    await loadContainers();
    const items = state.containers.filter(item => !state.warehouseId || String(item.warehouseId) === String(state.warehouseId));
    $("#operatorContainers").textContent = items.length;
    $("#operatorWaiting").textContent = items.filter(item => item.status === "WAITING_FOR_PLACEMENT").length;
    $("#operatorStored").textContent = items.filter(item => item.status === "STORED").length;
    $("#operatorClosed").textContent = items.filter(item => ["MERGED","REMOVED"].includes(item.status)).length;
}

async function loadOperatorReceiving() {
    $("#receiveForm").addEventListener("submit", async event => { event.preventDefault(); const data = formData(event.currentTarget); try { const item = await api("/operator/containers/receive", {method:"POST", body:{warehouseId:num(requireWarehouse()), containerNumber:data.containerNumber, articleNumber:data.articleNumber, quantity:num(data.quantity), weightKg:num(data.weightKg), heightMm:num(data.heightMm)}}); $("#receivingResult").className = "result-note"; $("#receivingResult").innerHTML = `<b>${escapeHtml(item.containerNumber)}</b> · ${escapeHtml(item.articleNumber)} · ${item.quantity} units ${badge(item.status)}`; event.currentTarget.reset(); setProcessStep(".process-steps", 3); toast("Pallet received"); } catch (error) { report(error); } });
    $("#receiveBatchForm").addEventListener("submit", async event => { event.preventDefault(); const rows = formData(event.currentTarget).rows.split(/\r?\n/).filter(Boolean).map(row => { const [containerNumber,articleNumber,quantity,weightKg,heightMm] = row.split(",").map(value => value.trim()); return {warehouseId:num(requireWarehouse()),containerNumber,articleNumber,quantity:num(quantity),weightKg:num(weightKg),heightMm:num(heightMm)}; }); try { const result = await api("/operator/containers/receive/batch", {method:"POST", body:{containers:rows}}); $("#receivingResult").className = "result-note"; $("#receivingResult").textContent = `Received pallets: ${result.receivedContainers}`; toast("Batch receiving completed"); } catch (error) { report(error); } });
}

function renderRecommendation(item) {
    state.recommendation = item;
    const target = item.recommendationType === "MERGE" ? item.targetContainerNumber : item.recommendedStoragePlaceCode;
    const scanTarget = item.recommendedStoragePlaceCode || item.targetStoragePlaceCode || "";
    const closed = ["ACCEPTED","REJECTED","EXPIRED"].includes(item.status);
    $("#recommendationCard").innerHTML = `<div class="recommendation-result"><div><p class="eyebrow">RECOMMENDED ACTION · ${escapeHtml(label(item.recommendationType))}</p><div class="recommendation-target">${escapeHtml(target)}</div><div class="recommendation-meta">${badge(item.status)}${item.distanceFromEntryMm != null ? `<span class="badge info">${(item.distanceFromEntryMm/1000).toFixed(1)} m from entrance</span>` : ""}${item.estimatedTimeSeconds != null ? `<span class="badge info">${item.estimatedTimeSeconds} sec</span>` : ""}</div><p>${escapeHtml(item.reason)}</p></div>${closed ? `<div class="success-state"><b>${item.status === "ACCEPTED" ? "Action confirmed" : "Recommendation closed"}</b><p>You can scan the next pallet.</p></div>` : `<div class="scan-confirmation"><label>Scan the target location<input id="targetScan" value="${escapeHtml(scanTarget)}" required></label><button class="button accent wide" data-recommend-approve="${escapeHtml(item.code)}">Confirm</button><button class="button ghost wide" data-recommend-reject="${escapeHtml(item.code)}">Reject</button></div>`}</div>`;
    setProcessStep(".placement-steps", item.status === "ACCEPTED" ? 3 : closed ? 0 : 2);
}

async function loadOperatorPlacement() {
    $("#recommendForm").addEventListener("submit", async event => { event.preventDefault(); setProcessStep(".placement-steps", 1); try { renderRecommendation(await api("/operator/placement/recommend", {method:"POST", body:formData(event.currentTarget)})); } catch (error) { setProcessStep(".placement-steps", 0); report(error); } });
}

function renderCurrentStep(step) {
    $("#currentStepCard").className = "result-note";
    $("#currentStepCard").innerHTML = `<p class="eyebrow">CURRENT STEP ${step.sequenceNumber}</p><h2>${escapeHtml(label(step.type))}</h2><dl class="detail-list compact"><div><dt>Pallet</dt><dd>${escapeHtml(step.sourceContainerNumber)}</dd></div><div><dt>From</dt><dd>${escapeHtml(step.fromStoragePlaceCode || "—")}</dd></div><div><dt>To</dt><dd>${escapeHtml(step.toStoragePlaceCode || step.targetContainerNumber || "—")}</dd></div></dl><p>${escapeHtml(step.reason)}</p>${badge(step.status)}`;
    const form = $("#completeStepForm"); form.elements.sourceContainerNumber.value = step.sourceContainerNumber || ""; form.elements.targetStoragePlaceCode.value = step.toStoragePlaceCode || ""; form.elements.targetContainerNumber.value = step.targetContainerNumber || "";
}

async function loadOperatorRelocation() {
    const initialPlan = new URLSearchParams(location.search).get("plan"); if (initialPlan) $("#currentStepForm [name=planCode]").value = initialPlan;
    $("#currentStepForm").addEventListener("submit", async event => { event.preventDefault(); const code = formData(event.currentTarget).planCode; try { renderCurrentStep(await api(`/operator/optimization-plans/${encodeURIComponent(code)}/steps/current`)); $("#completeStepForm [name=planCode]").value = code; } catch (error) { report(error); } });
    $("#completeStepForm").addEventListener("submit", async event => { event.preventDefault(); const data = formData(event.currentTarget); try { const result = await api(`/operator/optimization-plans/${encodeURIComponent(data.planCode)}/steps/current/complete`, {method:"POST", body:{sourceContainerNumber:data.sourceContainerNumber, targetStoragePlaceCode:data.targetStoragePlaceCode, targetContainerNumber:data.targetContainerNumber || null}}); if (result.nextStep) renderCurrentStep(result.nextStep); else $("#currentStepCard").innerHTML = '<div class="success-state"><b>Plan completed</b><p>All relocations have been confirmed.</p></div>'; toast("Relocation confirmed"); } catch (error) { report(error); } });
}

async function loadOperatorInventory() {
    await loadContainers();
    $("#containerSearch").addEventListener("input", renderContainers); $("#containerStatus").addEventListener("change", renderContainers); $("[data-load-containers]").addEventListener("click", () => loadContainers().catch(report));
    $("#containerForm").addEventListener("submit", async event => { event.preventDefault(); const data = formData(event.currentTarget); try { await api(`/operator/containers/${encodeURIComponent(data.containerNumber)}`, {method:"PATCH", body:{quantity:num(data.quantity), weightKg:num(data.weightKg), heightMm:num(data.heightMm)}}); $("#containerDialog").close(); await loadContainers(); toast("Pallet data updated"); } catch (error) { report(error); } });
}

function bindDelegatedActions() {
    document.addEventListener("click", async event => {
        const button = event.target.closest("button"); if (!button) return;
        try {
            if (button.dataset.route) await loadRoute(button.dataset.route);
            if (button.dataset.articleEdit) { const item = await api(`/admin/articles/${button.dataset.articleEdit}`); const form = $("#articleForm"); Object.entries(item).forEach(([key,value]) => { if (form.elements[key]) form.elements[key].value = value ?? ""; }); form.elements.id.value = item.id; form.elements.articleNumber.disabled = true; $("#articleDialogTitle").textContent = `Edit ${item.articleNumber}`; $("#articleDialog").showModal(); }
            if (button.dataset.articleDelete && confirm("Delete this article from the catalog?")) { await api(`/admin/articles/${button.dataset.articleDelete}`, {method:"DELETE"}); await loadArticles(); toast("Article deleted"); }
            if (button.dataset.accountEdit) { const item = await api(`/admin/accounts/${button.dataset.accountEdit}`); const form = $("#accountForm"); Object.entries(item).forEach(([key,value]) => { if (form.elements[key]) form.elements[key].value = value ?? ""; }); form.elements.id.value = item.id; form.elements.password.required = false; form.dataset.original = JSON.stringify({email:item.email, fullName:item.fullName, role:item.role}); $("#accountDialogTitle").textContent = `Edit ${item.fullName}`; $("#accountDialog").showModal(); }
            if (button.dataset.accountToggle) { const action = button.dataset.status === "ACTIVE" ? "deactivate" : "activate"; await api(`/admin/accounts/${button.dataset.accountToggle}/${action}`, {method:"PATCH"}); await loadAccounts(); toast("User status changed"); }
            if (button.dataset.planApprove) { renderPlan(await api(`/admin/optimization-plans/${encodeURIComponent(button.dataset.planApprove)}/approve`, {method:"POST"})); toast("Plan assigned to operator"); }
            if (button.dataset.planCancel) { renderPlan(await api(`/admin/optimization-plans/${encodeURIComponent(button.dataset.planCancel)}/cancel`, {method:"POST"})); toast("Plan cancelled"); }
            if (button.dataset.recommendApprove) { const scanned = $("#targetScan")?.value.trim(); if (!scanned) { toast("Scan the target location", "error"); return; } renderRecommendation(await api(`/operator/placement/recommendations/${encodeURIComponent(button.dataset.recommendApprove)}/approve`, {method:"POST", body:{scannedStoragePlaceCode:scanned}})); toast("Placement confirmed"); }
            if (button.dataset.recommendReject) { renderRecommendation(await api(`/operator/placement/recommendations/${encodeURIComponent(button.dataset.recommendReject)}/reject`, {method:"POST"})); toast("Recommendation rejected"); }
            if (button.dataset.containerEdit) { const item = await api(`/operator/containers/${encodeURIComponent(button.dataset.containerEdit)}`); const form = $("#containerForm"); form.elements.containerNumber.value = item.containerNumber; form.elements.quantity.value = item.quantity; form.elements.weightKg.value = item.weightKg; form.elements.heightMm.value = item.heightMm; $("#containerDialogTitle").textContent = `Pallet ${item.containerNumber}`; $("#containerDialog").showModal(); }
            if (button.dataset.containerRemove && confirm("Remove this pallet?")) { await api(`/operator/containers/${encodeURIComponent(button.dataset.containerRemove)}/remove`, {method:"PATCH"}); await loadContainers(); toast("Pallet removed"); }
        } catch (error) { report(error); }
    });
}

const pageInitializers = {
    "admin-dashboard": loadAdminDashboard,
    "admin-warehouse": loadWarehousePage,
    "admin-inventory": loadAdminInventory,
    "admin-demand": loadAdminDemand,
    "admin-optimization": loadAdminOptimization,
    "admin-accounts": loadAdminAccounts,
    "operator-dashboard": loadOperatorDashboard,
    "operator-receiving": loadOperatorReceiving,
    "operator-placement": loadOperatorPlacement,
    "operator-relocation": loadOperatorRelocation,
    "operator-inventory": loadOperatorInventory
};

const pageRefreshers = {
    "admin-dashboard": loadAdminDashboard,
    "admin-warehouse": refreshWarehouseData,
    "admin-inventory": () => Promise.all([loadArticles(), loadContainers()]),
    "admin-demand": refreshAdminDemandData,
    "admin-optimization": refreshAdminOptimizationData,
    "admin-accounts": loadAccounts,
    "operator-dashboard": loadOperatorDashboard,
    "operator-inventory": loadContainers
};

async function refreshCurrentPage() {
    const refresher = pageRefreshers[page];
    if (refresher) await refresher();
}

async function init() {
    bindShell(); bindDelegatedActions();
    try {
        await loadWarehouses();
        await pageInitializers[page]?.();
    } catch (error) { report(error); }
}

document.addEventListener("DOMContentLoaded", init);
