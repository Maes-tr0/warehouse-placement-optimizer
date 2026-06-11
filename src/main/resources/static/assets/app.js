"use strict";

const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
const state = {
    email: sessionStorage.getItem("wpo.email") || "",
    password: sessionStorage.getItem("wpo.password") || "",
    warehouses: [],
    warehouseId: sessionStorage.getItem("wpo.warehouseId") || "",
    warehouse: null,
    assessment: null,
    plan: null,
    recommendation: null,
    articles: [],
    containers: [],
    places: []
};

const endpointCatalog = [
    ["Warehouses · list", "GET", "/admin/warehouses", ""],
    ["Warehouses · create", "POST", "/admin/warehouses", '{"warehouseCode":"WH-01","warehouseName":"Main warehouse","layoutType":"MAIN_CORRIDOR_ONE_SIDE_AISLES","aisleCount":4,"rackRowCount":8,"baysPerRackRow":8,"palletPlacesPerLevel":3,"aisleWidthMm":3500,"levelProfiles":[{"levelNumber":1,"clearHeightMm":1800,"maxCellLoadKg":1000}],"maxBayLoadKg":9000}'],
    ["Warehouse · details", "GET", "/admin/warehouses/{warehouseId}", ""],
    ["Warehouse · storage places", "GET", "/admin/warehouses/{warehouseId}/storage-places", ""],
    ["Warehouse · route", "GET", "/admin/warehouses/{warehouseId}/routes/storage-places/{storagePlaceCode}", ""],
    ["Articles · list", "GET", "/admin/articles", ""],
    ["Articles · by id", "GET", "/admin/articles/{id}", ""],
    ["Articles · by number", "GET", "/admin/articles/number/{articleNumber}", ""],
    ["Articles · create", "POST", "/admin/articles", '{"articleNumber":"100001","name":"Sample article","unitType":"BOX","unitWidthMm":300,"unitLengthMm":400,"unitHeightMm":250,"unitWeightKg":8.5,"maxQuantityPerPallet":80}'],
    ["Articles · update", "PATCH", "/admin/articles/{id}", '{"name":"Updated article","unitType":"BOX","unitWidthMm":300,"unitLengthMm":400,"unitHeightMm":250,"unitWeightKg":8.5,"maxQuantityPerPallet":80}'],
    ["Articles · delete", "DELETE", "/admin/articles/{id}", ""],
    ["Articles · batch", "POST", "/admin/articles/batch", '{"articles":[{"articleNumber":"100001","name":"Article 1","unitType":"BOX","unitWidthMm":300,"unitLengthMm":400,"unitHeightMm":250,"unitWeightKg":8.5,"maxQuantityPerPallet":80}]}'],
    ["Containers · list", "GET", "/operator/containers", ""],
    ["Containers · by number", "GET", "/operator/containers/{containerNumber}", ""],
    ["Containers · receive", "POST", "/operator/containers/receive", '{"warehouseId":{warehouseId},"containerNumber":"CNT-001","articleNumber":"100001","quantity":40,"weightKg":320,"heightMm":1200}'],
    ["Containers · receive batch", "POST", "/operator/containers/receive/batch", '{"containers":[{"warehouseId":{warehouseId},"containerNumber":"CNT-001","articleNumber":"100001","quantity":40,"weightKg":320,"heightMm":1200}]}'],
    ["Containers · update", "PATCH", "/operator/containers/{containerNumber}", '{"quantity":35,"weightKg":280,"heightMm":1200}'],
    ["Containers · place", "POST", "/operator/containers/{containerNumber}/place", '{"storagePlaceCode":"A01-R01-B001-L1-P1"}'],
    ["Containers · merge", "POST", "/operator/containers/{containerNumber}/merge", '{"targetContainerNumber":"CNT-002"}'],
    ["Containers · remove", "PATCH", "/operator/containers/{containerNumber}/remove", ""],
    ["Placement · recommend", "POST", "/operator/placement/recommend", '{"containerNumber":"CNT-001"}'],
    ["Placement · approve", "POST", "/operator/placement/recommendations/{recommendationCode}/approve", '{"scannedStoragePlaceCode":"A01-R01-B001-L1-P1"}'],
    ["Placement · reject", "POST", "/operator/placement/recommendations/{recommendationCode}/reject", ""],
    ["Demand · import", "POST", "/admin/demand-history/import", '{"warehouseId":{warehouseId},"orders":[{"orderNumber":"ORD-001","orderDateTime":"2026-06-01T09:00:00","items":[{"articleNumber":"100001","quantity":10}]}]}'],
    ["Demand · articles", "GET", "/admin/warehouses/{warehouseId}/demand/articles", ""],
    ["Demand · top", "GET", "/admin/warehouses/{warehouseId}/demand/articles/top?limit=10", ""],
    ["Demand · article", "GET", "/admin/warehouses/{warehouseId}/demand/articles/{articleNumber}", ""],
    ["ML · train", "POST", "/admin/warehouses/{warehouseId}/demand-forecast-models/train", ""],
    ["ML · latest", "GET", "/admin/warehouses/{warehouseId}/demand-forecast-models/latest", ""],
    ["ML · history", "GET", "/admin/warehouses/{warehouseId}/demand-forecast-models", ""],
    ["Optimization · assess", "POST", "/admin/warehouses/{warehouseId}/optimization-assessments", ""],
    ["Optimization · latest assessment", "GET", "/admin/warehouses/{warehouseId}/optimization-assessments/latest", ""],
    ["Optimization · create plan", "POST", "/admin/optimization-plans/assessments/{assessmentId}", ""],
    ["Optimization · plan", "GET", "/admin/optimization-plans/{planCode}", ""],
    ["Optimization · approve plan", "POST", "/admin/optimization-plans/{planCode}/approve", ""],
    ["Optimization · cancel plan", "POST", "/admin/optimization-plans/{planCode}/cancel", ""],
    ["Relocation · current step", "GET", "/operator/optimization-plans/{planCode}/steps/current", ""],
    ["Relocation · complete step", "POST", "/operator/optimization-plans/{planCode}/steps/current/complete", '{"sourceContainerNumber":"CNT-001","targetStoragePlaceCode":"A01-R01-B001-L1-P1","targetContainerNumber":null}'],
    ["Movements · warehouse", "GET", "/admin/container-movements?warehouseId={warehouseId}", ""],
    ["Movements · container", "GET", "/admin/container-movements/containers/{containerNumber}", ""],
    ["Audit · search", "GET", "/admin/audit/events?warehouseCode={warehouseCode}&page=0&size=50", ""],
    ["System · health", "GET", "/actuator/health", ""],
    ["System · info", "GET", "/actuator/info", ""],
    ["System · metrics", "GET", "/actuator/metrics", ""],
    ["System · Prometheus", "GET", "/actuator/prometheus", ""]
];

class ApiError extends Error {
    constructor(status, payload) {
        super(payload?.message || `HTTP ${status}`);
        this.status = status;
        this.payload = payload;
    }
}

function credentials() {
    return `Basic ${btoa(unescape(encodeURIComponent(`${state.email}:${state.password}`)))}`;
}

async function api(path, options = {}) {
    const headers = new Headers(options.headers || {});
    if (state.email && state.password) headers.set("Authorization", credentials());
    if (options.body !== undefined && !(options.body instanceof FormData)) headers.set("Content-Type", "application/json");
    const response = await fetch(path, {
        method: options.method || "GET",
        headers,
        body: options.body === undefined ? undefined : JSON.stringify(options.body)
    });
    const contentType = response.headers.get("content-type") || "";
    let payload = null;
    if (response.status !== 204) payload = contentType.includes("json") ? await response.json() : await response.text();
    updateConsole(response, payload, options.method || "GET", path);
    if (!response.ok) throw new ApiError(response.status, payload);
    return payload;
}

function updateConsole(response, payload, method, path) {
    $("#responseStatus").textContent = `${response.status} ${response.statusText}`;
    $("#responseOutput").textContent = `${method} ${path}\n\n${typeof payload === "string" ? payload : JSON.stringify(payload, null, 2)}`;
}

function toast(message, type = "success") {
    const item = document.createElement("div");
    item.className = `toast ${type}`;
    item.textContent = message;
    $("#toastStack").append(item);
    setTimeout(() => item.remove(), 4200);
}

function report(error) {
    console.error(error);
    if (error instanceof ApiError) {
        if (error.status === 401) setConnected(false);
        toast(error.payload?.message || error.message, "error");
    } else {
        toast(error.message || "Невідома помилка", "error");
    }
}

function setConnected(connected) {
    $("#connectionDot").classList.toggle("connected", connected);
    $("#connectionText").textContent = connected ? "API підключено" : "Не підключено";
    $("#connectionUser").textContent = connected ? state.email : "Увійдіть у систему";
    $("#authButtonText").textContent = connected ? state.email.split("@")[0] : "Увійти";
    $("#userInitials").textContent = connected ? initials(state.email.split("@")[0]) : "?";
}

function initials(value) {
    return value.split(/[._ -]+/).filter(Boolean).slice(0, 2).map(part => part[0].toUpperCase()).join("") || "U";
}

function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>'"]/g, char => ({"&":"&amp;","<":"&lt;",">":"&gt;","'":"&#39;",'"':"&quot;"}[char]));
}

function formObject(form) {
    return Object.fromEntries(new FormData(form).entries());
}

function num(value) {
    return value === "" || value == null ? null : Number(value);
}

function formatDate(value) {
    if (!value) return "—";
    return new Intl.DateTimeFormat("uk-UA", {dateStyle: "medium", timeStyle: "short"}).format(new Date(value));
}

function badge(status) {
    const good = ["ACTIVE","AVAILABLE","STORED","ACCEPTED","HEALTHY","COMPLETED","PUBLISHED","SUCCEEDED"];
    const warn = ["WAITING_FOR_PLACEMENT","SUGGESTED","DRAFT","READY","IN_PROGRESS","OPTIMIZATION_RECOMMENDED","PROCESSING"];
    const bad = ["INACTIVE","REMOVED","REJECTED","CANCELLED","EXPIRED","FAILED"];
    const type = good.includes(status) ? "good" : warn.includes(status) ? "warn" : bad.includes(status) ? "bad" : "info";
    return `<span class="badge ${type}">${escapeHtml(status || "—")}</span>`;
}

function requireWarehouse() {
    if (!state.warehouseId) throw new Error("Спочатку виберіть активний склад");
    return state.warehouseId;
}

function resolveTemplate(value) {
    return value
        .replaceAll("{warehouseId}", state.warehouseId || "1")
        .replaceAll("{warehouseCode}", state.warehouse?.warehouseCode || state.warehouse?.code || "WH-01");
}

function navigate(viewName) {
    $$(".view").forEach(view => view.classList.toggle("active", view.id === `view-${viewName}`));
    $$(".nav-item").forEach(item => item.classList.toggle("active", item.dataset.view === viewName));
    const view = $(`#view-${viewName}`);
    $("#pageTitle").textContent = view.dataset.title;
    $("#pageEyebrow").textContent = view.dataset.eyebrow;
    location.hash = viewName;
    $("#sidebar").classList.remove("open");
    refreshView(viewName).catch(report);
}

async function connect(email, password) {
    state.email = email.trim();
    state.password = password;
    sessionStorage.setItem("wpo.email", state.email);
    sessionStorage.setItem("wpo.password", state.password);
    await api("/operator/containers");
    setConnected(true);
    $("#authDialog").close();
    await loadWarehouses();
    await refreshView(location.hash.slice(1) || "dashboard");
    toast("Підключення успішне");
}

function logout() {
    sessionStorage.removeItem("wpo.email");
    sessionStorage.removeItem("wpo.password");
    state.email = "";
    state.password = "";
    setConnected(false);
    $("#authDialog").close();
    toast("Дані входу очищено");
}

async function loadWarehouses() {
    try {
        state.warehouses = await api("/admin/warehouses");
        const select = $("#warehouseSelect");
        select.innerHTML = '<option value="">Не вибрано</option>' + state.warehouses.map(item =>
            `<option value="${item.id}">${escapeHtml(item.warehouseCode)} · ${escapeHtml(item.warehouseName)}</option>`
        ).join("");
        if (!state.warehouseId && state.warehouses.length) state.warehouseId = String(state.warehouses[0].id);
        select.value = state.warehouseId;
        if (state.warehouseId) {
            sessionStorage.setItem("wpo.warehouseId", state.warehouseId);
            await loadWarehouseSummary();
        }
    } catch (error) {
        if (error.status !== 403) throw error;
        toast("Оператор підключений. Адміністративні розділи недоступні.", "error");
    }
}

async function loadWarehouseSummary() {
    if (!state.warehouseId) return;
    state.warehouse = await api(`/admin/warehouses/${state.warehouseId}`);
    $("#heroWarehouseName").textContent = state.warehouse.warehouseName;
    $("#warehouseAisles").textContent = state.warehouse.aisleCount ?? "—";
    $("#warehouseRows").textContent = state.warehouse.rackRowCount ?? "—";
    $("#warehousePlaces").textContent = state.warehouse.storagePlaceCount ?? "—";
    $("#warehouseStatus").innerHTML = badge(state.warehouse.status);
    $("#metricPlaces").textContent = state.warehouse.storagePlaceCount ?? "—";
}

async function loadDashboard() {
    const warehouseId = requireWarehouse();
    await loadWarehouseSummary();
    const results = await Promise.allSettled([
        api(`/admin/warehouses/${warehouseId}/storage-places`),
        api("/operator/containers"),
        api("/admin/articles"),
        api(`/admin/warehouses/${warehouseId}/optimization-assessments/latest`),
        api(`/admin/warehouses/${warehouseId}/demand-forecast-models/latest`),
        api(`/admin/container-movements?warehouseId=${warehouseId}`),
        api(`/admin/warehouses/${warehouseId}/demand/articles/top?limit=8`)
    ]);
    const value = index => results[index].status === "fulfilled" ? results[index].value : null;
    const places = value(0) || [];
    const containers = value(1) || [];
    const articles = value(2) || [];
    const assessment = value(3);
    const model = value(4);
    const movements = value(5) || [];
    const demand = value(6) || [];
    $("#metricAvailable").textContent = `${places.filter(p => p.status === "AVAILABLE").length} доступно`;
    $("#metricContainers").textContent = containers.filter(c => !state.warehouseId || String(c.warehouseId) === String(state.warehouseId)).length;
    $("#metricWaiting").textContent = `${containers.filter(c => c.status === "WAITING_FOR_PLACEMENT").length} очікують розміщення`;
    $("#metricArticles").textContent = articles.length;
    if (assessment) renderAssessment(assessment);
    if (model) renderModel(model);
    renderRecentMovements(movements.slice(0, 5));
    renderDemandChart(demand);
}

function renderRecentMovements(items) {
    $("#recentMovements").classList.toggle("empty-state", !items.length);
    $("#recentMovements").innerHTML = items.length ? items.map(item => `<div class="timeline-item"><i></i><div><b>${escapeHtml(item.containerNumber)} · ${escapeHtml(item.type)}</b><small>${escapeHtml(item.fromStoragePlaceCode || "Приймання")} → ${escapeHtml(item.toStoragePlaceCode || item.targetContainerNumber || "—")}</small></div><time>${formatDate(item.performedAt)}</time></div>`).join("") : "Рухів поки немає";
}

function renderDemandChart(items) {
    const root = $("#topDemandChart");
    if (!items.length) { root.className = "bar-chart empty-state"; root.textContent = "Імпортуйте історію замовлень"; return; }
    root.className = "bar-chart";
    const max = Math.max(...items.map(item => item.demandScore), 1);
    root.innerHTML = items.map(item => `<div class="bar-item"><div style="height:${Math.max(18, item.demandScore / max * 180)}px" data-value="${item.demandScore.toFixed(2)}"></div><span>${escapeHtml(item.articleNumber)}</span></div>`).join("");
}

async function loadPlaces() {
    const warehouseId = requireWarehouse();
    const status = $("#placeStatusFilter").value;
    state.places = await api(`/admin/warehouses/${warehouseId}/storage-places${status ? `?status=${status}` : ""}`);
    renderPlaces();
}

function renderPlaces() {
    const query = $("#placeSearch").value.trim().toLowerCase();
    const items = state.places.filter(item => !query || item.code.toLowerCase().includes(query));
    $("#placesTable").innerHTML = items.length ? items.map(item => `<tr><td><b>${escapeHtml(item.code)}</b></td><td>${escapeHtml(item.rackRowCode)} / L${item.levelNumber}</td><td>${item.accessXMm}, ${item.accessYMm} мм</td><td>${(item.distanceFromEntryMm / 1000).toFixed(1)} м</td><td>${item.maxWeightKg} кг · ${item.maxHeightMm} мм</td><td>${badge(item.status)}</td><td><button class="mini-button" data-route="${escapeHtml(item.code)}">Маршрут</button></td></tr>`).join("") : '<tr><td colspan="7" class="empty-cell">Локацій не знайдено</td></tr>';
}

async function loadRoute(code) {
    const route = await api(`/admin/warehouses/${requireWarehouse()}/routes/storage-places/${encodeURIComponent(code)}`);
    $("#routeSummary").innerHTML = `<b>${escapeHtml(route.storagePlaceCode)}</b> · ${(route.distanceMm / 1000).toFixed(1)} м · приблизно ${route.estimatedTravelTimeSeconds} с`;
    const svg = $("#routeMap");
    if (!route.points?.length) { svg.innerHTML = ""; return; }
    const xs = route.points.map(p => p.xMm), ys = route.points.map(p => p.yMm);
    const minX = Math.min(...xs), maxX = Math.max(...xs), minY = Math.min(...ys), maxY = Math.max(...ys);
    const scaleX = x => 35 + (x - minX) / Math.max(maxX - minX, 1) * 650;
    const scaleY = y => 35 + (y - minY) / Math.max(maxY - minY, 1) * 190;
    const points = route.points.map(p => `${scaleX(p.xMm)},${scaleY(p.yMm)}`).join(" ");
    svg.innerHTML = `<polyline points="${points}" fill="none" stroke="#17231e" stroke-width="7" stroke-linecap="round" stroke-linejoin="round"/>${route.points.map((p, i) => `<circle cx="${scaleX(p.xMm)}" cy="${scaleY(p.yMm)}" r="${i === route.points.length - 1 ? 10 : 6}" fill="${i === route.points.length - 1 ? "#d9ff63" : "#57d39b"}" stroke="#17231e" stroke-width="3"/><text x="${scaleX(p.xMm) + 10}" y="${scaleY(p.yMm) - 10}" font-size="11" fill="#526057">${escapeHtml(p.label)}</text>`).join("")}`;
}

async function loadContainers() {
    state.containers = await api("/operator/containers");
    renderContainers();
}

function renderContainers() {
    const query = $("#containerSearch").value.trim().toLowerCase();
    const items = state.containers.filter(item => (!state.warehouseId || String(item.warehouseId) === String(state.warehouseId)) && (!query || `${item.containerNumber} ${item.articleNumber} ${item.articleName}`.toLowerCase().includes(query)));
    $("#containersTable").innerHTML = items.length ? items.map(item => `<tr><td><b>${escapeHtml(item.containerNumber)}</b></td><td>${escapeHtml(item.articleNumber)}<small class="block">${escapeHtml(item.articleName)}</small></td><td>${item.quantity}</td><td>${item.weightKg} кг · ${item.heightMm} мм</td><td>${escapeHtml(item.currentStoragePlaceCode || "—")}</td><td>${badge(item.status)}</td><td><div class="row-actions"><button class="mini-button" data-container-detail="${escapeHtml(item.containerNumber)}">Деталі</button><button class="mini-button" data-container-edit="${escapeHtml(item.containerNumber)}">Змінити</button><button class="mini-button danger" data-container-remove="${escapeHtml(item.containerNumber)}">Списати</button></div></td></tr>`).join("") : '<tr><td colspan="7" class="empty-cell">Контейнерів не знайдено</td></tr>';
}

async function loadArticles() {
    state.articles = await api("/admin/articles");
    $("#articlesTable").innerHTML = state.articles.length ? state.articles.map(item => `<tr><td><b>${escapeHtml(item.articleNumber)}</b></td><td>${escapeHtml(item.name)}</td><td>${item.unitType}</td><td>${item.unitWidthMm}×${item.unitLengthMm}×${item.unitHeightMm} мм</td><td>${item.unitWeightKg} кг</td><td>${item.maxQuantityPerPallet}</td><td><div class="row-actions"><button class="mini-button" data-article-number="${escapeHtml(item.articleNumber)}">Перевірити</button><button class="mini-button" data-article-edit="${item.id}">Змінити</button><button class="mini-button danger" data-article-delete="${item.id}">Видалити</button></div></td></tr>`).join("") : '<tr><td colspan="7" class="empty-cell">Артикулів немає</td></tr>';
}

async function loadDemand() {
    const data = formObject($("#demandFilterForm"));
    const params = new URLSearchParams();
    if (data.from) params.set("from", data.from);
    if (data.to) params.set("to", data.to);
    const items = await api(`/admin/warehouses/${requireWarehouse()}/demand/articles${params.size ? `?${params}` : ""}`);
    $("#demandTable").innerHTML = items.length ? items.slice(0, Number(data.limit || 20)).map(item => `<tr data-demand-article="${escapeHtml(item.articleNumber)}"><td>#${item.rank}</td><td><b>${escapeHtml(item.articleNumber)}</b><small class="block">${escapeHtml(item.articleName)}</small></td><td>${item.demandScore.toFixed(3)}</td><td>${item.historicalQuantity} / ${item.orderCount} зам.</td><td>${item.storedQuantity} · ${item.storedContainerCount} конт.</td><td>${item.averageDistanceFromEntryMm ? (item.averageDistanceFromEntryMm / 1000).toFixed(1) + " м" : "—"}</td><td>${badge(item.scoreSource)}</td></tr>`).join("") : '<tr><td colspan="7" class="empty-cell">Даних попиту немає</td></tr>';
}

function renderModel(model) {
    $("#metricModel").textContent = `v${model.versionNumber}`;
    $("#metricModelDate").textContent = formatDate(model.trainedAt);
    $("#modelVersion").textContent = `v${model.versionNumber} · ${model.status}`;
    $("#modelAlgorithm").textContent = model.algorithm || "—";
    $("#modelImprovement").textContent = model.improvementPercent == null ? "—" : `${model.improvementPercent.toFixed(1)}%`;
    $("#modelObservations").textContent = model.observationCount;
}

async function loadLatestModel() {
    const model = await api(`/admin/warehouses/${requireWarehouse()}/demand-forecast-models/latest`);
    renderModel(model);
    return model;
}

async function loadModelHistory() {
    const items = await api(`/admin/warehouses/${requireWarehouse()}/demand-forecast-models`);
    $("#modelsTable").innerHTML = items.length ? items.map(item => `<tr><td><b>v${item.versionNumber}</b><small class="block">${escapeHtml(item.code)}</small></td><td>${badge(item.status)}</td><td>${item.trainingStart} → ${item.trainingEnd}</td><td>${item.modelMae?.toFixed(3) ?? "—"}</td><td>${item.baselineMae?.toFixed(3) ?? "—"}</td><td>${item.improvementPercent?.toFixed(1) ?? "—"}%</td><td>${formatDate(item.trainedAt)}</td></tr>`).join("") : '<tr><td colspan="7" class="empty-cell">Моделей ще немає</td></tr>';
}

function renderAssessment(item) {
    state.assessment = item;
    const score = Number(item.scorePercent || 0);
    [$("#scoreRing"), $("#optimizationRing")].forEach(ring => ring.style.setProperty("--score", score));
    $("#scoreValue").textContent = `${score.toFixed(1)}%`;
    $("#optimizationScore").textContent = `${score.toFixed(1)}%`;
    $("#heroRecommendation").textContent = item.optimizationRecommended ? "Розміщення нижче допустимого порога. Потрібен план перестановок." : "Розміщення відповідає поточному профілю попиту.";
    $("#optimizationHeadline").textContent = item.optimizationRecommended ? "Рекомендовано оптимізацію" : item.status === "INSUFFICIENT_DATA" ? "Недостатньо історії попиту" : "Склад у здоровому стані";
    $("#optimizationDetails").textContent = `Поріг ${item.thresholdPercent}%. Проаналізовано ${item.analyzedContainerCount} контейнерів і ${item.demandObservationCount} спостережень.`;
    $("#assessmentDetails").innerHTML = `<div><dt>ID</dt><dd>${item.id}</dd></div><div><dt>Поріг</dt><dd>${item.thresholdPercent}%</dd></div><div><dt>Контейнери</dt><dd>${item.demandMatchedContainerCount}/${item.analyzedContainerCount}</dd></div><div><dt>Спостереження</dt><dd>${item.demandObservationCount}</dd></div><div><dt>Середня відстань</dt><dd>${item.weightedAverageDistanceMm ? (item.weightedAverageDistanceMm / 1000).toFixed(1) + " м" : "—"}</dd></div><div><dt>Час аналізу</dt><dd>${formatDate(item.analyzedAt)}</dd></div>`;
    $("#createPlanButton").disabled = !item.optimizationRecommended;
}

async function latestAssessment() {
    const item = await api(`/admin/warehouses/${requireWarehouse()}/optimization-assessments/latest`);
    renderAssessment(item);
    return item;
}

function renderPlan(plan) {
    state.plan = plan;
    $("#planQuickState").innerHTML = `<b>${escapeHtml(plan.code)}</b> · ${plan.status} · ${plan.completedSteps}/${plan.totalSteps} кроків`;
    $("#planCard").innerHTML = `<div class="section-head"><div><p class="eyebrow">${escapeHtml(plan.code)}</p><h2>План перестановок</h2></div>${badge(plan.status)}</div><div class="metric-grid compact"><article class="metric"><span>Початково</span><b>${plan.initialScorePercent}%</b></article><article class="metric"><span>Прогноз</span><b>${plan.projectedScorePercent}%</b></article><article class="metric"><span>Ціль</span><b>${plan.targetScorePercent}%</b></article><article class="metric"><span>Економія</span><b>${Math.round(plan.estimatedTimeSavingSeconds / 60)} хв</b></article></div><div class="button-row"><button class="button primary" data-plan-approve="${escapeHtml(plan.code)}" ${plan.status !== "DRAFT" ? "disabled" : ""}>Затвердити</button><button class="button ghost" data-plan-cancel="${escapeHtml(plan.code)}" ${["COMPLETED","CANCELLED"].includes(plan.status) ? "disabled" : ""}>Скасувати</button><button class="button subtle" data-execute-plan="${escapeHtml(plan.code)}">Виконувати</button></div><div class="step-list">${(plan.steps || []).map(step => `<div class="step-item"><span class="step-number">${step.sequenceNumber}</span><div><b>${escapeHtml(step.type)} · ${escapeHtml(step.sourceContainerNumber)}</b><small>${escapeHtml(step.fromStoragePlaceCode || "—")} → ${escapeHtml(step.toStoragePlaceCode || step.targetContainerNumber || "—")} · ${escapeHtml(step.reason)}</small></div>${badge(step.status)}</div>`).join("")}</div>`;
}

async function loadMovements(containerNumber = "") {
    const path = containerNumber ? `/admin/container-movements/containers/${encodeURIComponent(containerNumber)}` : `/admin/container-movements?warehouseId=${requireWarehouse()}`;
    const items = await api(path);
    $("#movementsTable").innerHTML = items.length ? items.map(item => `<tr><td>${formatDate(item.performedAt)}</td><td>${badge(item.type)}</td><td><b>${escapeHtml(item.containerNumber)}</b>${item.targetContainerNumber ? `<small class="block">→ ${escapeHtml(item.targetContainerNumber)}</small>` : ""}</td><td>${escapeHtml(item.articleNumber)}</td><td>${escapeHtml(item.fromStoragePlaceCode || "—")} → ${escapeHtml(item.toStoragePlaceCode || "—")}</td><td>${item.quantity}</td><td>${escapeHtml(item.performedBy)}</td></tr>`).join("") : '<tr><td colspan="7" class="empty-cell">Рухів не знайдено</td></tr>';
    renderRecentMovements(items.slice(0, 5));
}

async function loadAudit(eventType = "", size = 25) {
    const code = state.warehouse?.warehouseCode || state.warehouse?.code;
    if (!code) throw new Error("Не вдалося визначити код складу");
    const params = new URLSearchParams({warehouseCode: code, page: "0", size: String(size)});
    if (eventType) params.set("eventType", eventType);
    const result = await api(`/admin/audit/events?${params}`);
    const root = $("#auditEvents");
    root.classList.toggle("empty-state", !result.items.length);
    root.innerHTML = result.items.length ? result.items.map(item => `<article class="event-item"><header><div><b>${escapeHtml(item.eventType)}</b><small class="block">${escapeHtml(item.aggregateType)} · ${escapeHtml(item.aggregateId)}</small></div><time>${formatDate(item.occurredAt)}</time></header><pre>${escapeHtml(JSON.stringify(item.payload, null, 2))}</pre></article>`).join("") : "Подій не знайдено";
}

async function loadAccounts() {
    const items = await api("/admin/accounts");
    $("#accountsTable").innerHTML = items.length ? items.map(item => `<tr><td><b>${escapeHtml(item.fullName)}</b><small class="block">${escapeHtml(item.email)}</small></td><td>${badge(item.role)}</td><td>${badge(item.status)}</td><td>${formatDate(item.createdAt)}</td><td>${formatDate(item.updatedAt)}</td><td><div class="row-actions"><button class="mini-button" data-account-edit="${item.id}">Змінити</button><button class="mini-button" data-account-toggle="${item.id}" data-status="${item.status}">${item.status === "ACTIVE" ? "Деактивувати" : "Активувати"}</button></div></td></tr>`).join("") : '<tr><td colspan="6" class="empty-cell">Користувачів немає</td></tr>';
}

async function refreshView(view) {
    if (!state.email || !state.password) return;
    if (view === "dashboard") await loadDashboard();
    if (view === "warehouse") { await loadWarehouseSummary(); await loadPlaces(); }
    if (view === "inventory") await Promise.all([loadContainers(), loadArticles()]);
    if (view === "demand") await Promise.allSettled([loadDemand(), loadLatestModel(), loadModelHistory()]);
    if (view === "optimization") await latestAssessment().catch(error => { if (error.status !== 404) throw error; });
    if (view === "operations") await loadMovements();
    if (view === "accounts") await loadAccounts();
}

function bindNavigation() {
    $$(".nav-item").forEach(item => item.addEventListener("click", () => navigate(item.dataset.view)));
    $$('[data-go]').forEach(item => item.addEventListener("click", () => {
        navigate(item.dataset.go);
        if (item.dataset.focus) setTimeout(() => $(`#${item.dataset.focus}`)?.focus(), 80);
    }));
    $("#menuButton").addEventListener("click", () => $("#sidebar").classList.toggle("open"));
    $("#refreshButton").addEventListener("click", () => refreshView(location.hash.slice(1) || "dashboard").catch(report));
    $("#warehouseSelect").addEventListener("change", async event => {
        state.warehouseId = event.target.value;
        sessionStorage.setItem("wpo.warehouseId", state.warehouseId);
        state.assessment = null; state.plan = null;
        try { await loadWarehouseSummary(); await refreshView(location.hash.slice(1) || "dashboard"); } catch (error) { report(error); }
    });
    $$(".tab").forEach(tab => tab.addEventListener("click", () => {
        $$(".tab").forEach(item => item.classList.toggle("active", item === tab));
        $$(".tab-panel").forEach(panel => panel.classList.toggle("active", panel.id === `tab-${tab.dataset.tab}`));
    }));
}

function bindDialogs() {
    $$('[data-dialog]').forEach(button => button.addEventListener("click", () => {
        const dialog = $(`#${button.dataset.dialog}`);
        if (dialog.id === "articleDialog") resetArticleForm();
        if (dialog.id === "accountDialog") resetAccountForm();
        dialog.showModal();
    }));
    $$('[data-close]').forEach(button => button.addEventListener("click", () => button.closest("dialog").close()));
    $("#authButton").addEventListener("click", () => {
        $("#authForm [name=email]").value = state.email;
        $("#authDialog").showModal();
    });
    $("#logoutButton").addEventListener("click", logout);
    $("#authForm").addEventListener("submit", event => {
        event.preventDefault(); const data = formObject(event.currentTarget); connect(data.email, data.password).catch(report);
    });
}

function bindWarehouse() {
    $("#warehouseForm").addEventListener("submit", async event => {
        event.preventDefault(); const data = formObject(event.currentTarget);
        try {
            const created = await api("/admin/warehouses", {method:"POST", body:{...data, aisleCount:num(data.aisleCount), rackRowCount:num(data.rackRowCount), baysPerRackRow:num(data.baysPerRackRow), palletPlacesPerLevel:num(data.palletPlacesPerLevel), aisleWidthMm:num(data.aisleWidthMm), maxBayLoadKg:num(data.maxBayLoadKg), levelProfiles:JSON.parse(data.levelProfiles)}});
            $("#warehouseDialog").close(); await loadWarehouses(); $("#warehouseSelect").value = String(created.id); $("#warehouseSelect").dispatchEvent(new Event("change")); toast("Склад створено");
        } catch (error) { report(error); }
    });
    $("#loadPlacesButton").addEventListener("click", () => loadPlaces().catch(report));
    $("#placeSearch").addEventListener("input", renderPlaces);
    $("#placeStatusFilter").addEventListener("change", () => loadPlaces().catch(report));
    $("#routeForm").addEventListener("submit", event => { event.preventDefault(); loadRoute(formObject(event.currentTarget).storagePlaceCode).catch(report); });
}

function resetArticleForm() {
    $("#articleForm").reset(); $("#articleForm [name=id]").value = ""; $("#articleDialogTitle").textContent = "Новий артикул"; $("#articleForm [name=articleNumber]").disabled = false;
}

function resetAccountForm() {
    $("#accountForm").reset(); $("#accountForm [name=id]").value = ""; $("#accountDialogTitle").textContent = "Новий користувач"; $("#accountForm [name=password]").required = true;
}

function bindInventory() {
    $("#loadContainersButton").addEventListener("click", () => loadContainers().catch(report));
    $("#containerSearch").addEventListener("input", renderContainers);
    $("#receiveForm").addEventListener("submit", async event => {
        event.preventDefault(); const data = formObject(event.currentTarget);
        try { await api("/operator/containers/receive", {method:"POST", body:{warehouseId:num(requireWarehouse()), containerNumber:data.containerNumber, articleNumber:data.articleNumber, quantity:num(data.quantity), weightKg:num(data.weightKg), heightMm:num(data.heightMm)}}); event.currentTarget.reset(); await loadContainers(); toast("Контейнер прийнято"); } catch (error) { report(error); }
    });
    $("#receiveBatchForm").addEventListener("submit", async event => {
        event.preventDefault(); const rows = formObject(event.currentTarget).rows.split(/\r?\n/).filter(Boolean).map(row => { const [containerNumber,articleNumber,quantity,weightKg,heightMm] = row.split(",").map(v=>v.trim()); return {warehouseId:num(requireWarehouse()),containerNumber,articleNumber,quantity:num(quantity),weightKg:num(weightKg),heightMm:num(heightMm)}; });
        try { const result = await api("/operator/containers/receive/batch", {method:"POST", body:{containers:rows}}); await loadContainers(); toast(`Прийнято ${result.receivedContainers} контейнерів`); } catch (error) { report(error); }
    });
    $("#articleForm").addEventListener("submit", async event => {
        event.preventDefault(); const data = formObject(event.currentTarget); const id = data.id; delete data.id; if (!id) data.articleNumber = data.articleNumber.trim(); else delete data.articleNumber;
        ["unitWidthMm","unitLengthMm","unitHeightMm","unitWeightKg","maxQuantityPerPallet"].forEach(key => data[key] = num(data[key]));
        try { await api(id ? `/admin/articles/${id}` : "/admin/articles", {method:id ? "PATCH" : "POST", body:data}); $("#articleDialog").close(); await loadArticles(); toast("Артикул збережено"); } catch (error) { report(error); }
    });
}

function bindPlacement() {
    $("#recommendForm").addEventListener("submit", async event => {
        event.preventDefault(); try { state.recommendation = await api("/operator/placement/recommend", {method:"POST", body:formObject(event.currentTarget)}); renderRecommendation(); } catch (error) { report(error); }
    });
    $("#directPlaceForm").addEventListener("submit", async event => { event.preventDefault(); const data=formObject(event.currentTarget); try { await api(`/operator/containers/${encodeURIComponent(data.containerNumber)}/place`, {method:"POST",body:{storagePlaceCode:data.storagePlaceCode}}); toast("Контейнер розміщено"); event.currentTarget.reset(); } catch(error){report(error);} });
    $("#directMergeForm").addEventListener("submit", async event => { event.preventDefault(); const data=formObject(event.currentTarget); try { await api(`/operator/containers/${encodeURIComponent(data.sourceContainerNumber)}/merge`, {method:"POST",body:{targetContainerNumber:data.targetContainerNumber}}); toast("Контейнери об'єднано"); event.currentTarget.reset(); } catch(error){report(error);} });
}

function renderRecommendation() {
    const item = state.recommendation;
    const target = item.recommendationType === "MERGE" ? item.targetContainerNumber : item.recommendedStoragePlaceCode;
    $("#recommendationCard").innerHTML = `<div class="recommendation-result"><div><p class="eyebrow">${escapeHtml(item.recommendationType)} RECOMMENDATION</p><div class="recommendation-target">${escapeHtml(target)}</div><div class="recommendation-meta">${badge(item.status)}<span class="badge info">${(item.distanceFromEntryMm/1000).toFixed(1)} м</span><span class="badge info">${item.estimatedTimeSeconds} с</span><span class="badge info">score ${item.score}</span></div><p>${escapeHtml(item.reason)}</p></div><div class="button-row"><button class="button accent" data-recommend-approve="${escapeHtml(item.code)}" data-target="${escapeHtml(item.recommendedStoragePlaceCode || item.targetStoragePlaceCode || "")}">Підтвердити сканування</button><button class="button ghost" data-recommend-reject="${escapeHtml(item.code)}">Відхилити</button></div></div>`;
}

function bindDemand() {
    $("#demandFilterForm").addEventListener("submit", event => { event.preventDefault(); loadDemand().catch(report); });
    $("#loadModelHistoryButton").addEventListener("click", () => loadModelHistory().catch(report));
    $("#trainModelButton").addEventListener("click", async () => { try { const model=await api(`/admin/warehouses/${requireWarehouse()}/demand-forecast-models/train`,{method:"POST"}); renderModel(model); await loadModelHistory(); toast("Навчання моделі завершено"); } catch(error){report(error);} });
    $("#demandImportForm").addEventListener("submit", async event => { event.preventDefault(); try { const result=await api("/admin/demand-history/import",{method:"POST",body:{warehouseId:num(requireWarehouse()),orders:JSON.parse(formObject(event.currentTarget).orders)}}); $("#demandImportDialog").close(); toast(`Імпортовано ${result.importedOrders} замовлень`); await loadDemand(); } catch(error){report(error);} });
}

function bindOptimization() {
    const analyze = async () => { try { const item=await api(`/admin/warehouses/${requireWarehouse()}/optimization-assessments`,{method:"POST"}); renderAssessment(item); toast("Аналіз завершено"); } catch(error){report(error);} };
    $("#runAssessmentButton").addEventListener("click", analyze); $$('[data-action="analyze"]').forEach(button=>button.addEventListener("click", analyze));
    $("#latestAssessmentButton").addEventListener("click", () => latestAssessment().catch(report));
    $("#createPlanButton").addEventListener("click", async () => { try { const plan=await api(`/admin/optimization-plans/assessments/${state.assessment.id}`,{method:"POST"}); renderPlan(plan); toast("План створено"); } catch(error){report(error);} });
    $("#planLookupForm").addEventListener("submit", async event => { event.preventDefault(); try { renderPlan(await api(`/admin/optimization-plans/${encodeURIComponent(formObject(event.currentTarget).planCode)}`)); } catch(error){report(error);} });
}

function renderCurrentStep(step) {
    $("#currentStepCard").className = "result-note";
    $("#currentStepCard").innerHTML = `<p class="eyebrow">STEP ${step.sequenceNumber} · ${escapeHtml(step.type)}</p><h2>${escapeHtml(step.sourceContainerNumber)} → ${escapeHtml(step.toStoragePlaceCode || step.targetContainerNumber || "—")}</h2><p>${escapeHtml(step.reason)}</p>${badge(step.status)}`;
    const form = $("#completeStepForm"); form.elements.sourceContainerNumber.value=step.sourceContainerNumber || ""; form.elements.targetStoragePlaceCode.value=step.toStoragePlaceCode || ""; form.elements.targetContainerNumber.value=step.targetContainerNumber || "";
}

function bindOperations() {
    $("#currentStepForm").addEventListener("submit", async event => { event.preventDefault(); const code=formObject(event.currentTarget).planCode; try { const step=await api(`/operator/optimization-plans/${encodeURIComponent(code)}/steps/current`); $("#completeStepForm [name=planCode]").value=code; renderCurrentStep(step); } catch(error){report(error);} });
    $("#completeStepForm").addEventListener("submit", async event => { event.preventDefault(); const data=formObject(event.currentTarget); try { const result=await api(`/operator/optimization-plans/${encodeURIComponent(data.planCode)}/steps/current/complete`,{method:"POST",body:{sourceContainerNumber:data.sourceContainerNumber,targetStoragePlaceCode:data.targetStoragePlaceCode,targetContainerNumber:data.targetContainerNumber || null}}); toast("Крок підтверджено"); result.nextStep ? renderCurrentStep(result.nextStep) : $("#currentStepCard").innerHTML="<b>План виконано</b>"; await loadMovements(); } catch(error){report(error);} });
    $("#movementFilterForm").addEventListener("submit", event => { event.preventDefault(); loadMovements(formObject(event.currentTarget).containerNumber).catch(report); });
    $("#auditForm").addEventListener("submit", event => { event.preventDefault(); const data=formObject(event.currentTarget); loadAudit(data.eventType, Number(data.size)).catch(report); });
}

function bindAccounts() {
    $("#accountForm").addEventListener("submit", async event => { event.preventDefault(); const data=formObject(event.currentTarget), id=data.id; delete data.id; if(id && !data.password) delete data.password; try { await api(id?`/admin/accounts/${id}`:"/admin/accounts",{method:id?"PATCH":"POST",body:data}); $("#accountDialog").close(); await loadAccounts(); toast("Користувача збережено"); } catch(error){report(error);} });
}

function bindConsole() {
    const select=$("#endpointSelect"); select.innerHTML=endpointCatalog.map((entry,index)=>`<option value="${index}">${escapeHtml(entry[0])}</option>`).join("");
    const applyEndpoint=()=>{ const entry=endpointCatalog[Number(select.value)]; const form=$("#apiConsoleForm"); form.elements.method.value=entry[1]; form.elements.path.value=resolveTemplate(entry[2]); form.elements.body.value=resolveTemplate(entry[3]); };
    select.addEventListener("change",applyEndpoint); applyEndpoint();
    $("#apiConsoleForm").addEventListener("submit",async event=>{ event.preventDefault(); const data=formObject(event.currentTarget); try { await api(data.path,{method:data.method,body:data.body.trim()?JSON.parse(data.body):undefined}); toast("Запит виконано"); } catch(error){report(error);} });
    $("#copyResponseButton").addEventListener("click",()=>navigator.clipboard.writeText($("#responseOutput").textContent).then(()=>toast("Відповідь скопійовано")));
}

function bindDelegatedActions() {
    document.addEventListener("click", async event => {
        const button = event.target.closest("button"); if (!button) return;
        try {
            if (button.dataset.route) { navigate("warehouse"); $("#routeForm [name=storagePlaceCode]").value=button.dataset.route; await loadRoute(button.dataset.route); }
            if (button.dataset.articleNumber) await api(`/admin/articles/number/${encodeURIComponent(button.dataset.articleNumber)}`);
            if (button.dataset.articleEdit) { const item=await api(`/admin/articles/${button.dataset.articleEdit}`); resetArticleForm(); Object.entries(item).forEach(([key,value])=>{ if($("#articleForm").elements[key]) $("#articleForm").elements[key].value=value??""; }); $("#articleForm [name=id]").value=item.id; $("#articleForm [name=articleNumber]").disabled=true; $("#articleDialogTitle").textContent="Редагування артикулу"; $("#articleDialog").showModal(); }
            if (button.dataset.articleDelete && confirm("Видалити артикул?")) { await api(`/admin/articles/${button.dataset.articleDelete}`,{method:"DELETE"}); await loadArticles(); toast("Артикул видалено"); }
            if (button.dataset.containerDetail) await api(`/operator/containers/${encodeURIComponent(button.dataset.containerDetail)}`);
            if (button.dataset.containerEdit) { const current=await api(`/operator/containers/${encodeURIComponent(button.dataset.containerEdit)}`); const quantity=prompt("Нова кількість",current.quantity); if(quantity!==null){ await api(`/operator/containers/${encodeURIComponent(button.dataset.containerEdit)}`,{method:"PATCH",body:{quantity:Number(quantity),weightKg:current.weightKg,heightMm:current.heightMm}}); await loadContainers(); toast("Контейнер оновлено"); } }
            if (button.dataset.containerRemove && confirm("Позначити контейнер як REMOVED?")) { await api(`/operator/containers/${encodeURIComponent(button.dataset.containerRemove)}/remove`,{method:"PATCH"}); await loadContainers(); toast("Контейнер списано"); }
            if (button.dataset.recommendApprove) { const scanned=prompt("Відскануйте або введіть цільову локацію",button.dataset.target); if(scanned){ state.recommendation=await api(`/operator/placement/recommendations/${encodeURIComponent(button.dataset.recommendApprove)}/approve`,{method:"POST",body:{scannedStoragePlaceCode:scanned}}); renderRecommendation(); toast("Рекомендацію підтверджено"); } }
            if (button.dataset.recommendReject) { state.recommendation=await api(`/operator/placement/recommendations/${encodeURIComponent(button.dataset.recommendReject)}/reject`,{method:"POST"}); renderRecommendation(); toast("Рекомендацію відхилено"); }
            if (button.dataset.planApprove) { renderPlan(await api(`/admin/optimization-plans/${encodeURIComponent(button.dataset.planApprove)}/approve`,{method:"POST"})); toast("План затверджено"); }
            if (button.dataset.planCancel) { renderPlan(await api(`/admin/optimization-plans/${encodeURIComponent(button.dataset.planCancel)}/cancel`,{method:"POST"})); toast("План скасовано"); }
            if (button.dataset.executePlan) { navigate("operations"); $("#currentStepForm [name=planCode]").value=button.dataset.executePlan; $("#completeStepForm [name=planCode]").value=button.dataset.executePlan; }
            if (button.dataset.accountEdit) { const item=await api(`/admin/accounts/${button.dataset.accountEdit}`); resetAccountForm(); Object.entries(item).forEach(([key,value])=>{ if($("#accountForm").elements[key]) $("#accountForm").elements[key].value=value??""; }); $("#accountForm [name=id]").value=item.id; $("#accountForm [name=password]").required=false; $("#accountDialogTitle").textContent="Редагування користувача"; $("#accountDialog").showModal(); }
            if (button.dataset.accountToggle) { const action=button.dataset.status==="ACTIVE"?"deactivate":"activate"; await api(`/admin/accounts/${button.dataset.accountToggle}/${action}`,{method:"PATCH"}); await loadAccounts(); toast("Статус змінено"); }
        } catch(error) { report(error); }
    });
    $("#demandTable").addEventListener("click", event => { const row=event.target.closest("[data-demand-article]"); if(row) api(`/admin/warehouses/${requireWarehouse()}/demand/articles/${encodeURIComponent(row.dataset.demandArticle)}`).catch(report); });
}

function init() {
    bindNavigation(); bindDialogs(); bindWarehouse(); bindInventory(); bindPlacement(); bindDemand(); bindOptimization(); bindOperations(); bindAccounts(); bindConsole(); bindDelegatedActions();
    setConnected(Boolean(state.email && state.password));
    navigate(location.hash.slice(1) || "dashboard");
    if (state.email && state.password) loadWarehouses().then(() => refreshView(location.hash.slice(1) || "dashboard")).catch(report);
    else $("#authDialog").showModal();
}

document.addEventListener("DOMContentLoaded", init);
