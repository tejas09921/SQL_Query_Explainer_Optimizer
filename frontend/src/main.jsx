import React, { useEffect, useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  Activity,
  AlertTriangle,
  Clock,
  Database,
  History,
  Loader2,
  Play,
  RefreshCw,
  Server,
  Sparkles,
  Trash2
} from "lucide-react";
import "./styles.css";

const API_BASE = "";

const demoRequest = {
  connection: {
    host: "localhost",
    port: 5432,
    database: "demo",
    username: "demo",
    password: "demo",
    sslMode: "disable"
  },
  sql: "SELECT * FROM orders WHERE customer_id = 42",
  allowNonSelect: false,
  includeBuffers: true
};

const emptyResult = null;

function App() {
  const [form, setForm] = useState(demoRequest);
  const [result, setResult] = useState(emptyResult);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    loadHistory();
  }, []);

  const bottleneckCounts = useMemo(() => {
    const counts = { HIGH: 0, MEDIUM: 0, LOW: 0 };
    for (const bottleneck of result?.bottlenecks || []) {
      counts[bottleneck.severity] = (counts[bottleneck.severity] || 0) + 1;
    }
    return counts;
  }, [result]);

  function updateConnection(field, value) {
    setForm((current) => ({
      ...current,
      connection: {
        ...current.connection,
        [field]: field === "port" ? Number(value) : value
      }
    }));
  }

  function updateFlag(field, value) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  async function analyze(event) {
    event.preventDefault();
    setLoading(true);
    setError("");
    try {
      const response = await fetch(`${API_BASE}/api/v1/analyze`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(form)
      });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.message || "Analysis failed.");
      }
      setResult(data);
      await loadHistory();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function loadHistory() {
    setHistoryLoading(true);
    try {
      const response = await fetch(`${API_BASE}/api/v1/history?page=0&size=20`);
      const data = await response.json();
      setHistory(data.content || []);
    } catch {
      setHistory([]);
    } finally {
      setHistoryLoading(false);
    }
  }

  async function loadHistoryItem(id) {
    setError("");
    try {
      const response = await fetch(`${API_BASE}/api/v1/history/${id}`);
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.message || "Unable to load history item.");
      }
      setResult(data);
      setForm((current) => ({ ...current, sql: data.sql }));
    } catch (err) {
      setError(err.message);
    }
  }

  async function deleteHistoryItem(id) {
    await fetch(`${API_BASE}/api/v1/history/${id}`, { method: "DELETE" });
    if (result?.id === id) {
      setResult(null);
    }
    await loadHistory();
  }

  return (
    <main className="shell">
      <header className="topbar">
        <div>
          <div className="brand">
            <Database size={24} />
            <span>SQL Query Explainer + Optimizer</span>
          </div>
          <p>Run PostgreSQL EXPLAIN ANALYZE, inspect bottlenecks, and get practical rewrite ideas.</p>
        </div>
        <div className="statusPill">
          <Activity size={16} />
          Live API
        </div>
      </header>

      <section className="workspace">
        <form className="panel queryPanel" onSubmit={analyze}>
          <div className="panelHeader">
            <div>
              <h1>Analyze Query</h1>
              <p>Connect to a Postgres database and inspect the execution plan.</p>
            </div>
            <button className="iconButton" type="button" onClick={() => setForm(demoRequest)} title="Reset demo values">
              <RefreshCw size={18} />
            </button>
          </div>

          <div className="connectionGrid">
            <Field label="Host" value={form.connection.host} onChange={(value) => updateConnection("host", value)} />
            <Field label="Port" value={form.connection.port} type="number" onChange={(value) => updateConnection("port", value)} />
            <Field label="Database" value={form.connection.database} onChange={(value) => updateConnection("database", value)} />
            <Field label="Username" value={form.connection.username} onChange={(value) => updateConnection("username", value)} />
            <Field label="Password" value={form.connection.password} type="password" onChange={(value) => updateConnection("password", value)} />
            <Field label="SSL Mode" value={form.connection.sslMode} onChange={(value) => updateConnection("sslMode", value)} />
          </div>

          <label className="sqlLabel" htmlFor="sql">SQL</label>
          <textarea
            id="sql"
            value={form.sql}
            onChange={(event) => setForm((current) => ({ ...current, sql: event.target.value }))}
            spellCheck="false"
          />

          <div className="optionsRow">
            <label>
              <input
                type="checkbox"
                checked={form.includeBuffers}
                onChange={(event) => updateFlag("includeBuffers", event.target.checked)}
              />
              Include buffers
            </label>
            <label>
              <input
                type="checkbox"
                checked={form.allowNonSelect}
                onChange={(event) => updateFlag("allowNonSelect", event.target.checked)}
              />
              Allow non-select
            </label>
          </div>

          {error && (
            <div className="errorBox">
              <AlertTriangle size={18} />
              {error}
            </div>
          )}

          <button className="primaryButton" type="submit" disabled={loading}>
            {loading ? <Loader2 className="spin" size={18} /> : <Play size={18} />}
            Analyze
          </button>
        </form>

        <aside className="panel historyPanel">
          <div className="panelHeader compact">
            <div>
              <h2>History</h2>
              <p>Recent analyses saved by the API.</p>
            </div>
            <button className="iconButton" type="button" onClick={loadHistory} title="Refresh history">
              {historyLoading ? <Loader2 className="spin" size={18} /> : <History size={18} />}
            </button>
          </div>
          <div className="historyList">
            {history.length === 0 && <div className="emptyState">No saved analyses yet.</div>}
            {history.map((item) => (
              <button className="historyItem" key={item.id} type="button" onClick={() => loadHistoryItem(item.id)}>
                <span>{item.sql}</span>
                <small>{formatMs(item.executionTimeMs)} execution</small>
                <Trash2
                  size={16}
                  onClick={(event) => {
                    event.stopPropagation();
                    deleteHistoryItem(item.id);
                  }}
                />
              </button>
            ))}
          </div>
        </aside>
      </section>

      <section className="resultsGrid">
        <div className="panel resultPanel">
          <div className="panelHeader compact">
            <div>
              <h2>Summary</h2>
              <p>Execution time and detected bottlenecks.</p>
            </div>
            <Sparkles size={22} />
          </div>
          {!result ? (
            <div className="emptyState tall">Run an analysis to see plan details.</div>
          ) : (
            <>
              <div className="metricGrid">
                <Metric icon={<Clock size={18} />} label="Planning" value={formatMs(result.planningTimeMs)} />
                <Metric icon={<Clock size={18} />} label="Execution" value={formatMs(result.executionTimeMs)} />
                <Metric icon={<AlertTriangle size={18} />} label="High" value={bottleneckCounts.HIGH} tone="high" />
                <Metric icon={<AlertTriangle size={18} />} label="Medium" value={bottleneckCounts.MEDIUM} tone="medium" />
              </div>
              <div className="explanation">{result.explanation}</div>
            </>
          )}
        </div>

        <div className="panel resultPanel">
          <div className="panelHeader compact">
            <div>
              <h2>Bottlenecks</h2>
              <p>Heuristic or LLM-ranked issues.</p>
            </div>
            <Server size={22} />
          </div>
          <div className="bottleneckList">
            {(result?.bottlenecks || []).length === 0 && <div className="emptyState">No bottlenecks to show.</div>}
            {(result?.bottlenecks || []).map((item, index) => (
              <div className="bottleneck" key={`${item.nodeType}-${index}`}>
                <span className={`severity ${item.severity?.toLowerCase()}`}>{item.severity}</span>
                <strong>{item.nodeType}{item.relationName ? ` on ${item.relationName}` : ""}</strong>
                <p>{item.issue}</p>
                <small>{item.metric}</small>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="resultsGrid bottom">
        <div className="panel">
          <div className="panelHeader compact">
            <div>
              <h2>Suggestions</h2>
              <p>Potential query or index improvements.</p>
            </div>
          </div>
          <div className="suggestionList">
            {(result?.suggestions || []).length === 0 && <div className="emptyState">No suggestions yet.</div>}
            {(result?.suggestions || []).map((suggestion, index) => (
              <article className="suggestion" key={`${suggestion.title}-${index}`}>
                <h3>{suggestion.title || "Suggestion"}</h3>
                <p>{suggestion.rationale}</p>
                {suggestion.estimatedSpeedup && <span>{suggestion.estimatedSpeedup}</span>}
                {suggestion.rewrittenSql && <pre>{suggestion.rewrittenSql}</pre>}
              </article>
            ))}
          </div>
        </div>

        <div className="panel">
          <div className="panelHeader compact">
            <div>
              <h2>Plan Tree</h2>
              <p>Recursive PostgreSQL execution plan.</p>
            </div>
          </div>
          <div className="planTree">
            {result?.planTree ? <PlanNode node={result.planTree} /> : <div className="emptyState">No plan loaded.</div>}
          </div>
        </div>
      </section>
    </main>
  );
}

function Field({ label, value, onChange, type = "text" }) {
  return (
    <label className="field">
      <span>{label}</span>
      <input type={type} value={value} onChange={(event) => onChange(event.target.value)} />
    </label>
  );
}

function Metric({ icon, label, value, tone = "" }) {
  return (
    <div className={`metric ${tone}`}>
      {icon}
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function PlanNode({ node }) {
  return (
    <div className="planNode">
      <div className="planLine">
        <strong>{node.nodeType}</strong>
        {node.relationName && <span>{node.relationName}</span>}
        <small>{formatMs(node.actualTotalTime)} · {node.percentOfTotalTime?.toFixed?.(1) || "0.0"}%</small>
      </div>
      {node.filter && <code>{node.filter}</code>}
      {(node.children || []).map((child, index) => (
        <PlanNode node={child} key={`${child.nodeType}-${index}`} />
      ))}
    </div>
  );
}

function formatMs(value) {
  const number = Number(value || 0);
  return `${number.toFixed(number >= 10 ? 1 : 2)} ms`;
}

createRoot(document.getElementById("root")).render(<App />);
