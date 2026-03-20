import { modelCards } from '../data/mockData'
import {
  IconBoxes,
  IconFlask,
  IconForum,
  IconHome,
  IconPackage,
  IconTrophy,
  IconUsers,
} from './icons.jsx'

const leftMenuItems = [
  { id: 'recommend', zh: '为你推荐', en: 'For You', icon: IconHome, active: true },
  { id: 'creator-circle', zh: '创作者圈子', en: 'Creator Circle', icon: IconUsers },
  { id: 'creator-fans', zh: '创作者与粉丝', en: 'Creators & Fans', icon: IconForum },
  { id: 'showcase', zh: '作品展示', en: 'Showcase', icon: IconTrophy },
  { id: 'discover', zh: '发现广场', en: 'Discover', icon: IconBoxes },
]

const composeActions = [
  { id: 'photo', zh: '图片', en: 'Photo', icon: IconPackage },
  { id: 'video', zh: '视频', en: 'Video', icon: IconFlask },
  { id: 'poll', zh: '投票', en: 'Poll', icon: IconTrophy },
  { id: 'topic', zh: '话题', en: 'Topic', icon: IconForum },
  { id: 'model', zh: '模型', en: 'Model', icon: IconBoxes },
  { id: 'post', zh: '文章', en: 'Post', icon: IconHome },
]

const hotTopics = [
  { id: 1, title: 'Weekly Roundup', count: 0, highlight: true },
  { id: 2, title: 'Easter2026', count: 0, highlight: true },
  { id: 3, title: 'Questions', count: 718 },
  { id: 4, title: 'Contests', count: 657 },
  { id: 5, title: 'Newmodel', count: 651 },
  { id: 6, title: 'Sharing Makes', count: 637 },
  { id: 7, title: 'Vote', count: 337 },
  { id: 8, title: 'Milestone', count: 223 },
  { id: 9, title: 'Comingsoon', count: 194 },
  { id: 10, title: 'Hostedbycreator', count: 173 },
]

function sectionTitle(zh, zhText, enText) {
  return zh ? zhText : enText
}

const COMPACT_ACTIVITY_PREVIEW_RATIO = '192 / 187'

export default function CommunityBoard({ locale = 'zh' }) {
  const zh = locale === 'zh'

  const postImages = [
    modelCards[2]?.image,
    modelCards[6]?.image,
    modelCards[3]?.image,
    modelCards[7]?.image,
  ].filter(Boolean)

  const activityBanner = modelCards[4]?.image || 'https://picsum.photos/seed/community-activity/780/520'

  return (
    <section className="px-6 pb-8 pt-2">
      <div className="grid grid-cols-1 items-start gap-5 xl:grid-cols-[246px_minmax(0,1fr)_320px]">
        <aside className="rounded-2xl border border-slate-200 bg-white p-3">
          <nav className="space-y-1.5">
            {leftMenuItems.map((item) => {
              const Icon = item.icon
              return (
                <button
                  key={item.id}
                  type="button"
                  className={`flex w-full items-center gap-3 rounded-xl px-3 py-3 text-left ${
                    item.active
                      ? 'bg-slate-100 font-semibold text-slate-900'
                      : 'text-slate-700 hover:bg-slate-50'
                  }`}
                >
                  <Icon className="h-4 w-4 shrink-0" />
                  <span>{zh ? item.zh : item.en}</span>
                </button>
              )
            })}
          </nav>
        </aside>

        <div className="space-y-4">
          <section className="rounded-2xl border border-slate-200 bg-white p-4">
            <div className="flex items-center gap-3">
              <img
                src="https://picsum.photos/seed/community-user-avatar/64/64"
                alt="User avatar"
                className="h-10 w-10 rounded-full border border-slate-200 object-cover"
              />
              <div className="flex-1 rounded-xl border border-slate-200 bg-slate-50 px-4 py-2.5 text-sm text-slate-400">
                {sectionTitle(zh, '分享你的故事...', 'Share your story...')}
              </div>
            </div>

            <div className="mt-4 flex flex-wrap items-center gap-3">
              {composeActions.map((item) => {
                const Icon = item.icon
                return (
                  <button
                    key={item.id}
                    type="button"
                    className="inline-flex items-center gap-1.5 rounded-lg px-2 py-1.5 text-sm text-slate-600 hover:bg-slate-100"
                  >
                    <Icon className="h-4 w-4" />
                    <span>{zh ? item.zh : item.en}</span>
                  </button>
                )
              })}

              <div className="ml-auto flex items-center gap-2">
                <button
                  type="button"
                  className="rounded-lg px-2 py-1.5 text-sm text-slate-600 hover:bg-slate-100"
                >
                  {sectionTitle(zh, '可见范围', 'Visibility')} ▼
                </button>
                <button
                  type="button"
                  className="rounded-lg bg-emerald-500 px-7 py-2 text-sm font-semibold text-white hover:bg-emerald-600"
                >
                  {sectionTitle(zh, '发布', 'Post')}
                </button>
              </div>
            </div>
          </section>

          <article className="rounded-2xl border border-slate-200 bg-white p-4">
            <header className="flex items-center justify-between gap-3">
              <div className="flex items-center gap-3">
                <img
                  src="https://picsum.photos/seed/community-author-avatar/80/80"
                  alt="Mercyless"
                  className="h-11 w-11 rounded-full object-cover"
                />
                <div>
                  <h3 className="text-xl font-semibold leading-tight text-slate-900">Mercyless</h3>
                  <p className="text-sm text-slate-500">@Mercyless · 2026/03/17 02:12</p>
                </div>
              </div>
              <button type="button" className="rounded-md px-2 text-slate-400 hover:bg-slate-100">
                ...
              </button>
            </header>

            <div className="mt-4 space-y-3 text-[16px] leading-8 text-slate-800">
              <p>
                So, I wanted to design something a bit more "evil" than just a fake spider for this April Fools'
                Day...
              </p>
              <p>
                I came up with this: The Ultimate Diver&apos;s Lung Tester. It looks like a legit precision instrument,
                but there&apos;s one catch: it&apos;s a total trap.
              </p>
            </div>

            <div className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-3">
              {postImages.map((imageUrl, index) => (
                <img
                  key={imageUrl}
                  src={imageUrl}
                  alt={`Post media ${index + 1}`}
                  className="h-56 w-full rounded-xl object-cover"
                />
              ))}
            </div>

            <a
              href="#"
              className="mt-3 flex items-center gap-3 rounded-xl bg-slate-50 p-2 transition hover:bg-slate-100"
            >
              <img
                src={postImages[0]}
                alt="Diver's Lung Tester"
                className="h-16 w-24 rounded-md object-cover"
              />
              <div>
                <h4 className="text-lg font-semibold text-slate-900">Diver&apos;s Lung Tester (April Fools Prank)</h4>
                <p className="text-sm text-slate-600">
                  <span className="font-semibold text-emerald-600">{sectionTitle(zh, '模型', 'Model')}</span> Mercyless
                </p>
              </div>
            </a>

            <footer className="mt-3 flex items-center justify-between text-sm text-slate-500">
              <button type="button" className="rounded-md px-2 py-1 hover:bg-slate-100">
                {sectionTitle(zh, '分享', 'Share')}
              </button>
              <div className="flex items-center gap-5">
                <span>👍 8</span>
                <span>💬 1</span>
                <span>↗ 0</span>
              </div>
            </footer>
          </article>
        </div>

        <aside className="space-y-4">
          <section
            className="overflow-hidden rounded-2xl border border-slate-200 bg-white"
            data-testid="community-activity-preview-card"
          >
            <header className="border-b border-slate-200 px-4 py-3">
              <h3 className="text-[32px] font-semibold text-slate-900">{sectionTitle(zh, '热门', 'Hot')}</h3>
            </header>

            <div className="space-y-2 p-3">
              {hotTopics.map((item, index) => (
                <div
                  key={item.id}
                  className={`flex items-center justify-between rounded-lg px-3 py-2 ${
                    item.highlight
                      ? 'bg-rose-50'
                      : index <= 4
                        ? 'bg-emerald-50'
                        : ''
                  }`}
                >
                  <p className="truncate font-semibold text-slate-800">
                    {index >= 2 ? `${index - 1}. ` : ''}
                    {item.title}
                  </p>
                  <div className="ml-2 shrink-0 text-sm text-slate-500">
                    {item.count > 0 ? item.count : '🔥'}
                  </div>
                </div>
              ))}
            </div>
          </section>

          <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white" data-testid="community-activity-preview-card">
            <header className="border-b border-slate-200 px-4 py-3">
              <h3 className="text-[32px] font-semibold text-slate-900">{sectionTitle(zh, '活动', 'Events')}</h3>
            </header>
            <div
              className="flex flex-col items-center gap-3 px-4 pb-4 pt-3"
              data-testid="community-activity-preview-content"
            >
              <div
                className="w-[65%] max-w-[192px] overflow-hidden rounded-xl"
                style={{ aspectRatio: COMPACT_ACTIVITY_PREVIEW_RATIO }}
                data-testid="community-activity-preview-image-frame"
              >
                <img
                  src={activityBanner}
                  alt="Easter Challenge"
                  className="h-full w-full object-cover"
                />
              </div>
              <p
                className="text-center text-base font-semibold leading-6 text-slate-900"
                data-testid="community-activity-preview-title"
              >
                {sectionTitle(zh, '春季创作挑战：发帖赢奖励', 'Share Your Easter Magic - Post & Win Rewards')}
              </p>
            </div>
          </section>
        </aside>
      </div>
    </section>
  )
}
