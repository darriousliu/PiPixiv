import { cacheLife } from "next/cache";
import { siteConfig } from "./site";

export type AssetKey = "android-default" | "ios" | "windows" | "macos" | "linux";

export interface ReleaseAsset {
  url: string;
  size: number;
}

export interface ReleaseInfo {
  tag: string;
  url: string;
  publishedAt: string;
  assets: Partial<Record<AssetKey, ReleaseAsset>>;
}

export interface RepoStats {
  stars: number;
  downloads: number;
}

const API = `https://api.github.com/repos/${siteConfig.repo}`;

/** Release 附件名形如 `PiPixiv-v2.7.0-android-default.apk`。 */
const ASSET_RULES: [AssetKey, RegExp][] = [
  ["android-default", /android-default\.apk$/i],
  ["ios", /\.ipa$/i],
  ["windows", /windows.*\.msi$/i],
  ["macos", /macos.*\.dmg$/i],
  ["linux", /linux.*\.tar\.gz$/i],
];

interface GitHubAsset {
  name: string;
  size: number;
  download_count: number;
  browser_download_url: string;
}

interface GitHubRelease {
  tag_name: string;
  html_url: string;
  published_at: string;
  assets: GitHubAsset[];
}

function headers(): HeadersInit {
  const h: Record<string, string> = {
    Accept: "application/vnd.github+json",
    "X-GitHub-Api-Version": "2022-11-28",
    "User-Agent": "pipixiv-website",
  };
  // 可选：提高未认证请求每小时 60 次的额度。
  if (process.env.GITHUB_TOKEN) h.Authorization = `Bearer ${process.env.GITHUB_TOKEN}`;
  return h;
}

async function getJSON<T>(path: string): Promise<T | null> {
  try {
    const res = await fetch(`${API}${path}`, { headers: headers() });
    if (!res.ok) return null;
    return (await res.json()) as T;
  } catch {
    return null;
  }
}

/**
 * 最新 Release 及各平台的下载链接。
 * 每小时在后台刷新；请求失败（如触发频率限制）时几分钟内重试。
 */
export async function getLatestRelease(): Promise<ReleaseInfo | null> {
  "use cache";

  const data = await getJSON<GitHubRelease>("/releases/latest");
  if (!data) {
    cacheLife("minutes");
    return null;
  }
  cacheLife("hours");

  const assets: ReleaseInfo["assets"] = {};
  for (const asset of data.assets ?? []) {
    const rule = ASSET_RULES.find(([, re]) => re.test(asset.name));
    if (rule && !assets[rule[0]]) {
      assets[rule[0]] = { url: asset.browser_download_url, size: asset.size };
    }
  }
  return { tag: data.tag_name, url: data.html_url, publishedAt: data.published_at, assets };
}

/**
 * Star 数以及所有 Release 的累计下载量。
 * 每小时在后台刷新；请求失败时几分钟内重试。
 */
export async function getRepoStats(): Promise<RepoStats | null> {
  "use cache";

  const repo = await getJSON<{ stargazers_count: number }>("");
  if (!repo) {
    cacheLife("minutes");
    return null;
  }
  cacheLife("hours");

  let downloads = 0;
  for (let page = 1; page <= 5; page++) {
    const releases = await getJSON<GitHubRelease[]>(`/releases?per_page=100&page=${page}`);
    if (!releases) break;
    for (const release of releases) {
      for (const asset of release.assets ?? []) downloads += asset.download_count ?? 0;
    }
    if (releases.length < 100) break;
  }
  return { stars: repo.stargazers_count, downloads };
}
