# Illustrations

The empty states use illustrations from [unDraw](https://undraw.co/) by Katerina Limpitsouni. unDraw illustrations are free for personal and commercial use without attribution. Check the licence on the unDraw site before adding more.

## Files

Each illustration is an Android vector drawable in `app/src/main/res/drawable/` named `illustration_<screen>.xml`:

| Drawable | Used by |
|---|---|
| `illustration_chats` | Chats, no conversations |
| `illustration_conversation` | Conversation, no messages |
| `illustration_contacts` | Contacts, no contacts |
| `illustration_search` | Any search with no results |
| `illustration_members` | Members, no members; add members to a group, nobody to add |
| `illustration_group_no_group` | Groups, no groups |
| `illustration_group_no_member` | Group, no members |
| `illustration_audit_log` | Audit log, no activity |

## Colors

The drawables do not hold fixed colors. They refer to the roles in `values/illustration_colors.xml`, and `values-night/illustration_colors.xml` redefines them for the dark theme. The accent role is the brand primary (`#415F91` in the light theme, `#AAC7FF` in the dark theme). The skin tones are fixed. The illustrations do not follow the contrast levels or dynamic color.

## Adding or replacing an illustration

1. Download the SVG from unDraw with its default colors.
2. Convert it to a vector drawable. Replace the unDraw colors with the roles above: accent `#6c63ff`, ink `#090814`, `#2f2e41` and `#3f3d56`, surface `#fff`, surface variant `#f2f2f2`, outline `#e6e6e6` and `#e4e4e4`, strong outline `#ccc`, `#cacaca` and `#d6d6e3`.
3. Show it with `EmptyState` (`ui/common/EmptyState.kt`) and add the drawable to `IllustrationResourcesTest`.
