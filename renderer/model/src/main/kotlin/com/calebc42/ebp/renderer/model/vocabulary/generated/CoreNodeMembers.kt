// SPDX-License-Identifier: GPL-3.0-or-later
// GENERATED from ebp/contract.json (contract_format 12, spec 3.1.0-draft) by
// tools/gen-node-members.py. DO NOT EDIT: CoreNodeMembersDriftTest pins it.
package com.calebc42.ebp.renderer.model.vocabulary.generated

import com.calebc42.ebp.renderer.model.vocabulary.ActionMember
import com.calebc42.ebp.renderer.model.vocabulary.AnyMember
import com.calebc42.ebp.renderer.model.vocabulary.ArrayMember
import com.calebc42.ebp.renderer.model.vocabulary.BooleanMember
import com.calebc42.ebp.renderer.model.vocabulary.BooleanOrAutoMember
import com.calebc42.ebp.renderer.model.vocabulary.ChildNodeMember
import com.calebc42.ebp.renderer.model.vocabulary.ColorMember
import com.calebc42.ebp.renderer.model.vocabulary.DpMember
import com.calebc42.ebp.renderer.model.vocabulary.EnumMember
import com.calebc42.ebp.renderer.model.vocabulary.IdentifierMember
import com.calebc42.ebp.renderer.model.vocabulary.IntMember
import com.calebc42.ebp.renderer.model.vocabulary.NodeArrayMember
import com.calebc42.ebp.renderer.model.vocabulary.NodeMember
import com.calebc42.ebp.renderer.model.vocabulary.NodeMemberSet
import com.calebc42.ebp.renderer.model.vocabulary.NumberArrayMember
import com.calebc42.ebp.renderer.model.vocabulary.NumberMember
import com.calebc42.ebp.renderer.model.vocabulary.ObjectMember
import com.calebc42.ebp.renderer.model.vocabulary.ScalarMember
import com.calebc42.ebp.renderer.model.vocabulary.StringMember
import com.calebc42.ebp.renderer.model.vocabulary.StringOrNumberMember
import com.calebc42.ebp.renderer.model.vocabulary.TwoNumberArrayMember

/** Typed members of the `text` node. */
object TextMembers : NodeMemberSet("text") {
    val Text: StringMember = StringMember("text", required = true)
    val Style: EnumMember<TextStyle> = EnumMember("style", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.TextStyle)
    val FontWeight: StringOrNumberMember = StringOrNumberMember("font_weight", required = false, tag = "font-weight")
    val Color: ColorMember = ColorMember("color", required = false)
    val Selectable: BooleanMember = BooleanMember("selectable", required = false)
    val MaxLines: IntMember = IntMember("max_lines", required = false, tag = "positive-integer", minimum = 1L, maximum = 9007199254740991L)
    val Syntax: IdentifierMember = IdentifierMember("syntax", required = false)
    override val required: List<NodeMember<*>> = listOf(Text)
    override val optional: List<NodeMember<*>> = listOf(Style, FontWeight, Color, Selectable, MaxLines, Syntax)
}

/** Typed members of the `rich_text` node. */
object RichTextMembers : NodeMemberSet("rich_text") {
    val Spans: ArrayMember = ArrayMember("spans", required = true, tag = "rich-span-array")
    val Style: EnumMember<RichTextStyle> = EnumMember("style", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.RichTextStyle)
    override val required: List<NodeMember<*>> = listOf(Spans)
    override val optional: List<NodeMember<*>> = listOf(Style)
}

/** Typed members of the `icon` node. */
object IconMembers : NodeMemberSet("icon") {
    val Name: StringMember = StringMember("name", required = true)
    val Size: DpMember = DpMember("size", required = false)
    val Color: ColorMember = ColorMember("color", required = false)
    val Badge: StringOrNumberMember = StringOrNumberMember("badge", required = false, tag = "string-or-number")
    val ContentDescription: StringMember = StringMember("content_description", required = false)
    override val required: List<NodeMember<*>> = listOf(Name)
    override val optional: List<NodeMember<*>> = listOf(Size, Color, Badge, ContentDescription)
}

/** Typed members of the `image` node. */
object ImageMembers : NodeMemberSet("image") {
    val Url: StringMember = StringMember("url", required = true)
    val ContentDescription: StringMember = StringMember("content_description", required = false)
    val ContentScale: EnumMember<ImageContentScale> = EnumMember("content_scale", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.ImageContentScale)
    override val required: List<NodeMember<*>> = listOf(Url)
    override val optional: List<NodeMember<*>> = listOf(ContentDescription, ContentScale)
}

/** Typed members of the `date_stamp` node. */
object DateStampMembers : NodeMemberSet("date_stamp") {
    val Day: IntMember = IntMember("day", required = false, tag = "integer-1-31", minimum = 1L, maximum = 31L)
    val Month: StringMember = StringMember("month", required = false)
    val MonthIndex: IntMember = IntMember("month_index", required = false, tag = "integer-1-12", minimum = 1L, maximum = 12L)
    val Year: IntMember = IntMember("year", required = false, tag = "non-negative-integer", minimum = 0L, maximum = 9007199254740991L)
    val Time: StringMember = StringMember("time", required = false)
    override val required: List<NodeMember<*>> = listOf()
    override val optional: List<NodeMember<*>> = listOf(Day, Month, MonthIndex, Year, Time)
}

/** Typed members of the `section_header` node. */
object SectionHeaderMembers : NodeMemberSet("section_header") {
    val Title: StringMember = StringMember("title", required = true)
    val Trailing: ChildNodeMember = ChildNodeMember("trailing", required = false)
    override val required: List<NodeMember<*>> = listOf(Title)
    override val optional: List<NodeMember<*>> = listOf(Trailing)
}

/** Typed members of the `empty_state` node. */
object EmptyStateMembers : NodeMemberSet("empty_state") {
    val Icon: IdentifierMember = IdentifierMember("icon", required = false)
    val Title: StringMember = StringMember("title", required = false)
    val Caption: StringMember = StringMember("caption", required = false)
    val ActionLabel: StringMember = StringMember("action_label", required = false)
    val OnTap: ActionMember = ActionMember("on_tap", required = false)
    override val required: List<NodeMember<*>> = listOf()
    override val optional: List<NodeMember<*>> = listOf(Icon, Title, Caption, ActionLabel, OnTap)
}

/** Typed members of the `progress` node. */
object ProgressMembers : NodeMemberSet("progress") {
    val Variant: EnumMember<ProgressVariant> = EnumMember("variant", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.ProgressVariant)
    val Value: NumberMember = NumberMember("value", required = false)
    override val required: List<NodeMember<*>> = listOf()
    override val optional: List<NodeMember<*>> = listOf(Variant, Value)
}

/** Typed members of the `badge` node. */
object BadgeMembers : NodeMemberSet("badge") {
    val Label: StringMember = StringMember("label", required = true)
    val Icon: IdentifierMember = IdentifierMember("icon", required = false)
    val Color: ColorMember = ColorMember("color", required = false)
    val Children: NodeArrayMember = NodeArrayMember("children", required = false)
    override val required: List<NodeMember<*>> = listOf(Label)
    override val optional: List<NodeMember<*>> = listOf(Icon, Color, Children)
}

/** Typed members of the `row` node. */
object RowMembers : NodeMemberSet("row") {
    val Children: NodeArrayMember = NodeArrayMember("children", required = true)
    val Spacing: DpMember = DpMember("spacing", required = false)
    val Align: EnumMember<RowAlign> = EnumMember("align", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.RowAlign)
    val Arrange: EnumMember<Arrange> = EnumMember("arrange", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.Arrange)
    val Scroll: BooleanMember = BooleanMember("scroll", required = false)
    val Fill: BooleanMember = BooleanMember("fill", required = false)
    val Overlap: DpMember = DpMember("overlap", required = false)
    val ContentPadding: DpMember = DpMember("content_padding", required = false)
    override val required: List<NodeMember<*>> = listOf(Children)
    override val optional: List<NodeMember<*>> = listOf(Spacing, Align, Arrange, Scroll, Fill, Overlap, ContentPadding)
}

/** Typed members of the `column` node. */
object ColumnMembers : NodeMemberSet("column") {
    val Children: NodeArrayMember = NodeArrayMember("children", required = true)
    val Spacing: DpMember = DpMember("spacing", required = false)
    val Align: EnumMember<ColumnAlign> = EnumMember("align", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.ColumnAlign)
    val Arrange: EnumMember<Arrange> = EnumMember("arrange", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.Arrange)
    val Scroll: BooleanMember = BooleanMember("scroll", required = false)
    val Fill: BooleanMember = BooleanMember("fill", required = false)
    val ReverseScroll: BooleanMember = BooleanMember("reverse_scroll", required = false)
    val Overlap: DpMember = DpMember("overlap", required = false)
    override val required: List<NodeMember<*>> = listOf(Children)
    override val optional: List<NodeMember<*>> = listOf(Spacing, Align, Arrange, Scroll, Fill, ReverseScroll, Overlap)
}

/** Typed members of the `flow_row` node. */
object FlowRowMembers : NodeMemberSet("flow_row") {
    val Children: NodeArrayMember = NodeArrayMember("children", required = true)
    val Spacing: DpMember = DpMember("spacing", required = false)
    val RunSpacing: DpMember = DpMember("run_spacing", required = false)
    val Align: EnumMember<FlowRowAlign> = EnumMember("align", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.FlowRowAlign)
    val Arrange: EnumMember<Arrange> = EnumMember("arrange", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.Arrange)
    override val required: List<NodeMember<*>> = listOf(Children)
    override val optional: List<NodeMember<*>> = listOf(Spacing, RunSpacing, Align, Arrange)
}

/** Typed members of the `box` node. */
object BoxMembers : NodeMemberSet("box") {
    val Children: NodeArrayMember = NodeArrayMember("children", required = true)
    val Alignment: EnumMember<BoxAlignment> = EnumMember("alignment", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.BoxAlignment)
    val OnTap: ActionMember = ActionMember("on_tap", required = false)
    val OnLongTap: ActionMember = ActionMember("on_long_tap", required = false)
    override val required: List<NodeMember<*>> = listOf(Children)
    override val optional: List<NodeMember<*>> = listOf(Alignment, OnTap, OnLongTap)
}

/** Typed members of the `surface` node. */
object SurfaceMembers : NodeMemberSet("surface") {
    val Children: NodeArrayMember = NodeArrayMember("children", required = true)
    val Color: ColorMember = ColorMember("color", required = false)
    val Shape: EnumMember<SurfaceShape> = EnumMember("shape", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.SurfaceShape)
    val Elevation: DpMember = DpMember("elevation", required = false)
    val ShadowElevation: DpMember = DpMember("shadow_elevation", required = false)
    override val required: List<NodeMember<*>> = listOf(Children)
    override val optional: List<NodeMember<*>> = listOf(Color, Shape, Elevation, ShadowElevation)
}

/** Typed members of the `lazy_column` node. */
object LazyColumnMembers : NodeMemberSet("lazy_column") {
    val Children: NodeArrayMember = NodeArrayMember("children", required = true)
    val Spacing: DpMember = DpMember("spacing", required = false)
    val ContentPadding: DpMember = DpMember("content_padding", required = false)
    override val required: List<NodeMember<*>> = listOf(Children)
    override val optional: List<NodeMember<*>> = listOf(Spacing, ContentPadding)
}

/** Typed members of the `variant_host` node. */
object VariantHostMembers : NodeMemberSet("variant_host") {
    val Id: IdentifierMember = IdentifierMember("id", required = true)
    val Value: IdentifierMember = IdentifierMember("value", required = true)
    val Variants: ArrayMember = ArrayMember("variants", required = true, tag = "variant-array")
    override val required: List<NodeMember<*>> = listOf(Id, Value, Variants)
    override val optional: List<NodeMember<*>> = listOf()
}

/** Typed members of the `spacer` node. */
object SpacerMembers : NodeMemberSet("spacer") {
    override val required: List<NodeMember<*>> = listOf()
    override val optional: List<NodeMember<*>> = listOf()
}

/** Typed members of the `divider` node. */
object DividerMembers : NodeMemberSet("divider") {
    val Color: ColorMember = ColorMember("color", required = false)
    val Thickness: DpMember = DpMember("thickness", required = false)
    override val required: List<NodeMember<*>> = listOf()
    override val optional: List<NodeMember<*>> = listOf(Color, Thickness)
}

/** Typed members of the `card` node. */
object CardMembers : NodeMemberSet("card") {
    val Children: NodeArrayMember = NodeArrayMember("children", required = true)
    val OnTap: ActionMember = ActionMember("on_tap", required = false)
    val OnLongTap: ActionMember = ActionMember("on_long_tap", required = false)
    val SwipeStart: ObjectMember = ObjectMember("swipe_start", required = false, tag = "swipe-object")
    val SwipeEnd: ObjectMember = ObjectMember("swipe_end", required = false, tag = "swipe-object")
    val Variant: EnumMember<CardVariant> = EnumMember("variant", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.CardVariant)
    override val required: List<NodeMember<*>> = listOf(Children)
    override val optional: List<NodeMember<*>> = listOf(OnTap, OnLongTap, SwipeStart, SwipeEnd, Variant)
}

/** Typed members of the `collapsible` node. */
object CollapsibleMembers : NodeMemberSet("collapsible") {
    val Id: IdentifierMember = IdentifierMember("id", required = true)
    val Header: ChildNodeMember = ChildNodeMember("header", required = true)
    val Children: NodeArrayMember = NodeArrayMember("children", required = true)
    val Collapsed: BooleanMember = BooleanMember("collapsed", required = false)
    val OnLongTap: ActionMember = ActionMember("on_long_tap", required = false)
    val SwipeStart: ObjectMember = ObjectMember("swipe_start", required = false, tag = "swipe-object")
    val SwipeEnd: ObjectMember = ObjectMember("swipe_end", required = false, tag = "swipe-object")
    override val required: List<NodeMember<*>> = listOf(Id, Header, Children)
    override val optional: List<NodeMember<*>> = listOf(Collapsed, OnLongTap, SwipeStart, SwipeEnd)
}

/** Typed members of the `reorderable_list` node. */
object ReorderableListMembers : NodeMemberSet("reorderable_list") {
    val Items: NodeArrayMember = NodeArrayMember("items", required = true)
    val OnReorder: ActionMember = ActionMember("on_reorder", required = false)
    override val required: List<NodeMember<*>> = listOf(Items)
    override val optional: List<NodeMember<*>> = listOf(OnReorder)
}

/** Typed members of the `tabs` node. */
object TabsMembers : NodeMemberSet("tabs") {
    val Items: ArrayMember = ArrayMember("items", required = true, tag = "tab-item-array")
    val Children: NodeArrayMember = NodeArrayMember("children", required = true)
    val Initial: IntMember = IntMember("initial", required = false, tag = "non-negative-integer", minimum = 0L, maximum = 9007199254740991L, default = 0)
    val Scrollable: BooleanMember = BooleanMember("scrollable", required = false)
    val PagerOnly: BooleanMember = BooleanMember("pager_only", required = false)
    val OnChange: ActionMember = ActionMember("on_change", required = false)
    val Id: IdentifierMember = IdentifierMember("id", required = false)
    val Style: EnumMember<TabsStyle> = EnumMember("style", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.TabsStyle)
    val Indicator: ObjectMember = ObjectMember("indicator", required = false, tag = "tab-indicator-object")
    override val required: List<NodeMember<*>> = listOf(Items, Children)
    override val optional: List<NodeMember<*>> = listOf(Initial, Scrollable, PagerOnly, OnChange, Id, Style, Indicator)
}

/** Typed members of the `tab_selector` node. */
object TabSelectorMembers : NodeMemberSet("tab_selector") {
    val Items: ArrayMember = ArrayMember("items", required = true, tag = "tab-item-array")
    val Selected: IntMember = IntMember("selected", required = true, tag = "non-negative-integer", minimum = 0L, maximum = 9007199254740991L)
    val OnChange: ActionMember = ActionMember("on_change", required = true)
    val Scrollable: BooleanMember = BooleanMember("scrollable", required = false)
    val Id: IdentifierMember = IdentifierMember("id", required = false)
    val Style: EnumMember<TabSelectorStyle> = EnumMember("style", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.TabSelectorStyle)
    val Indicator: ObjectMember = ObjectMember("indicator", required = false, tag = "tab-indicator-object")
    override val required: List<NodeMember<*>> = listOf(Items, Selected, OnChange)
    override val optional: List<NodeMember<*>> = listOf(Scrollable, Id, Style, Indicator)
}

/** Typed members of the `table` node. */
object TableMembers : NodeMemberSet("table") {
    val Rows: ArrayMember = ArrayMember("rows", required = true, tag = "table-row-array")
    val Aligns: ArrayMember = ArrayMember("aligns", required = false, tag = "align-array")
    val OnAddRow: ActionMember = ActionMember("on_add_row", required = false)
    val OnAddCol: ActionMember = ActionMember("on_add_col", required = false)
    override val required: List<NodeMember<*>> = listOf(Rows)
    override val optional: List<NodeMember<*>> = listOf(Aligns, OnAddRow, OnAddCol)
}

/** Typed members of the `button` node. */
object ButtonMembers : NodeMemberSet("button") {
    val Label: StringMember = StringMember("label", required = true)
    val OnTap: ActionMember = ActionMember("on_tap", required = true)
    val Icon: IdentifierMember = IdentifierMember("icon", required = false)
    val Variant: EnumMember<ButtonVariant> = EnumMember("variant", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.ButtonVariant)
    val Enabled: BooleanMember = BooleanMember("enabled", required = false)
    val Size: EnumMember<ButtonSize> = EnumMember("size", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.ButtonSize)
    val Shape: EnumMember<ButtonShape> = EnumMember("shape", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.ButtonShape)
    val AnimateShape: BooleanMember = BooleanMember("animate_shape", required = false)
    val Checked: BooleanMember = BooleanMember("checked", required = false)
    val OnChange: ActionMember = ActionMember("on_change", required = false)
    val Expanded: BooleanOrAutoMember = BooleanOrAutoMember("expanded", required = false)
    val CheckedShape: EnumMember<ButtonCheckedShape> = EnumMember("checked_shape", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.ButtonCheckedShape)
    val ShapeRole: EnumMember<ButtonShapeRole> = EnumMember("shape_role", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.ButtonShapeRole)
    val CheckedIcon: IdentifierMember = IdentifierMember("checked_icon", required = false)
    val Color: ColorMember = ColorMember("color", required = false)
    override val required: List<NodeMember<*>> = listOf(Label, OnTap)
    override val optional: List<NodeMember<*>> = listOf(Icon, Variant, Enabled, Size, Shape, AnimateShape, Checked, OnChange, Expanded, CheckedShape, ShapeRole, CheckedIcon, Color)
}

/** Typed members of the `icon_button` node. */
object IconButtonMembers : NodeMemberSet("icon_button") {
    val Icon: IdentifierMember = IdentifierMember("icon", required = true)
    val OnTap: ActionMember = ActionMember("on_tap", required = true)
    val ContentDescription: StringMember = StringMember("content_description", required = false)
    val Badge: StringOrNumberMember = StringOrNumberMember("badge", required = false, tag = "string-or-number")
    val Enabled: BooleanMember = BooleanMember("enabled", required = false)
    val Variant: EnumMember<IconButtonVariant> = EnumMember("variant", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.IconButtonVariant)
    val Checked: BooleanMember = BooleanMember("checked", required = false)
    val OnChange: ActionMember = ActionMember("on_change", required = false)
    val CheckedIcon: IdentifierMember = IdentifierMember("checked_icon", required = false)
    val Size: EnumMember<IconButtonSize> = EnumMember("size", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.IconButtonSize)
    val Shape: EnumMember<IconButtonShape> = EnumMember("shape", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.IconButtonShape)
    val WidthMode: EnumMember<IconButtonWidthMode> = EnumMember("width_mode", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.IconButtonWidthMode)
    val Color: ColorMember = ColorMember("color", required = false)
    override val required: List<NodeMember<*>> = listOf(Icon, OnTap)
    override val optional: List<NodeMember<*>> = listOf(ContentDescription, Badge, Enabled, Variant, Checked, OnChange, CheckedIcon, Size, Shape, WidthMode, Color)
}

/** Typed members of the `chip` node. */
object ChipMembers : NodeMemberSet("chip") {
    val Label: StringMember = StringMember("label", required = true)
    val OnTap: ActionMember = ActionMember("on_tap", required = false)
    val Selected: BooleanMember = BooleanMember("selected", required = false, default = false)
    val Icon: IdentifierMember = IdentifierMember("icon", required = false)
    val Enabled: BooleanMember = BooleanMember("enabled", required = false)
    val Variant: EnumMember<ChipVariant> = EnumMember("variant", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.ChipVariant)
    val TrailingIcon: IdentifierMember = IdentifierMember("trailing_icon", required = false)
    val Avatar: IdentifierMember = IdentifierMember("avatar", required = false)
    val ContentSpacing: DpMember = DpMember("content_spacing", required = false)
    override val required: List<NodeMember<*>> = listOf(Label)
    override val optional: List<NodeMember<*>> = listOf(OnTap, Selected, Icon, Enabled, Variant, TrailingIcon, Avatar, ContentSpacing)
}

/** Typed members of the `menu` node. */
object MenuMembers : NodeMemberSet("menu") {
    val Items: ArrayMember = ArrayMember("items", required = false, tag = "menu-item-array")
    val Groups: ArrayMember = ArrayMember("groups", required = false, tag = "menu-group-array")
    val Footer: ChildNodeMember = ChildNodeMember("footer", required = false)
    val Icon: IdentifierMember = IdentifierMember("icon", required = false)
    val Enabled: BooleanMember = BooleanMember("enabled", required = false)
    val InitialScroll: EnumMember<MenuInitialScroll> = EnumMember("initial_scroll", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.MenuInitialScroll)
    override val required: List<NodeMember<*>> = listOf()
    override val optional: List<NodeMember<*>> = listOf(Items, Groups, Footer, Icon, Enabled, InitialScroll)
}

/** Typed members of the `text_input` node. */
object TextInputMembers : NodeMemberSet("text_input") {
    val Id: IdentifierMember = IdentifierMember("id", required = true)
    val Value: StringMember = StringMember("value", required = false)
    val Hint: StringMember = StringMember("hint", required = false)
    val Label: StringMember = StringMember("label", required = false)
    val OnChange: ActionMember = ActionMember("on_change", required = false)
    val OnSubmit: ActionMember = ActionMember("on_submit", required = false)
    val SingleLine: BooleanMember = BooleanMember("single_line", required = false)
    val MinLines: IntMember = IntMember("min_lines", required = false, tag = "positive-integer", minimum = 1L, maximum = 9007199254740991L)
    val MaxLines: IntMember = IntMember("max_lines", required = false, tag = "positive-integer", minimum = 1L, maximum = 9007199254740991L)
    val Monospace: BooleanMember = BooleanMember("monospace", required = false)
    val Syntax: IdentifierMember = IdentifierMember("syntax", required = false)
    val Password: BooleanMember = BooleanMember("password", required = false)
    val Keyboard: EnumMember<TextInputKeyboard> = EnumMember("keyboard", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.TextInputKeyboard)
    val Autofocus: BooleanMember = BooleanMember("autofocus", required = false)
    val ClearOnSubmit: BooleanMember = BooleanMember("clear_on_submit", required = false)
    val Enabled: BooleanMember = BooleanMember("enabled", required = false)
    val Variant: EnumMember<TextInputVariant> = EnumMember("variant", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.TextInputVariant, default = com.calebc42.ebp.renderer.model.vocabulary.generated.TextInputVariant.Outlined)
    val IsError: BooleanMember = BooleanMember("is_error", required = false)
    val SupportingText: StringMember = StringMember("supporting_text", required = false)
    val Prefix: StringMember = StringMember("prefix", required = false)
    val Suffix: StringMember = StringMember("suffix", required = false)
    val LeadingIcon: IdentifierMember = IdentifierMember("leading_icon", required = false)
    val TrailingIcon: IdentifierMember = IdentifierMember("trailing_icon", required = false)
    val MaxLength: IntMember = IntMember("max_length", required = false, tag = "positive-integer", minimum = 1L, maximum = 9007199254740991L)
    val Selection: TwoNumberArrayMember = TwoNumberArrayMember("selection", required = false)
    val HideKeyboardOnSubmit: BooleanMember = BooleanMember("hide_keyboard_on_submit", required = false)
    val ContentPadding: DpMember = DpMember("content_padding", required = false)
    val Mask: StringMember = StringMember("mask", required = false)
    val Filter: EnumMember<TextInputFilter> = EnumMember("filter", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.TextInputFilter)
    override val required: List<NodeMember<*>> = listOf(Id)
    override val optional: List<NodeMember<*>> = listOf(Value, Hint, Label, OnChange, OnSubmit, SingleLine, MinLines, MaxLines, Monospace, Syntax, Password, Keyboard, Autofocus, ClearOnSubmit, Enabled, Variant, IsError, SupportingText, Prefix, Suffix, LeadingIcon, TrailingIcon, MaxLength, Selection, HideKeyboardOnSubmit, ContentPadding, Mask, Filter)
}

/** Typed members of the `editor` node. */
object EditorMembers : NodeMemberSet("editor") {
    val Id: IdentifierMember = IdentifierMember("id", required = true)
    val Document: IdentifierMember = IdentifierMember("document", required = false)
    val Value: StringMember = StringMember("value", required = false)
    val OnSave: ActionMember = ActionMember("on_save", required = false)
    val OnEnter: ActionMember = ActionMember("on_enter", required = false)
    val SingleLine: BooleanMember = BooleanMember("single_line", required = false)
    val MinLines: IntMember = IntMember("min_lines", required = false, tag = "positive-integer", minimum = 1L, maximum = 9007199254740991L)
    val MaxLines: IntMember = IntMember("max_lines", required = false, tag = "positive-integer", minimum = 1L, maximum = 9007199254740991L)
    val ReadOnly: BooleanMember = BooleanMember("read_only", required = false)
    val Syntax: IdentifierMember = IdentifierMember("syntax", required = false)
    val LineNumbers: BooleanMember = BooleanMember("line_numbers", required = false)
    val Complete: BooleanMember = BooleanMember("complete", required = false)
    val Chromeless: BooleanMember = BooleanMember("chromeless", required = false)
    val PublishState: BooleanMember = BooleanMember("publish_state", required = false)
    val Autofocus: BooleanMember = BooleanMember("autofocus", required = false)
    val Toolbar: AnyMember = AnyMember("toolbar", required = false, tag = "toolbar-id-or-item-array")
    val Enabled: BooleanMember = BooleanMember("enabled", required = false)
    override val required: List<NodeMember<*>> = listOf(Id)
    override val optional: List<NodeMember<*>> = listOf(Document, Value, OnSave, OnEnter, SingleLine, MinLines, MaxLines, ReadOnly, Syntax, LineNumbers, Complete, Chromeless, PublishState, Autofocus, Toolbar, Enabled)
}

/** Typed members of the `checkbox` node. */
object CheckboxMembers : NodeMemberSet("checkbox") {
    val Id: IdentifierMember = IdentifierMember("id", required = true)
    val Checked: BooleanMember = BooleanMember("checked", required = false, default = false)
    val State: EnumMember<CheckboxState> = EnumMember("state", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.CheckboxState)
    val Stroke: ObjectMember = ObjectMember("stroke", required = false, tag = "stroke-object")
    val Label: StringMember = StringMember("label", required = false)
    val OnChange: ActionMember = ActionMember("on_change", required = false)
    val Enabled: BooleanMember = BooleanMember("enabled", required = false)
    override val required: List<NodeMember<*>> = listOf(Id)
    override val optional: List<NodeMember<*>> = listOf(Checked, State, Stroke, Label, OnChange, Enabled)
}

/** Typed members of the `switch` node. */
object SwitchMembers : NodeMemberSet("switch") {
    val Id: IdentifierMember = IdentifierMember("id", required = true)
    val Checked: BooleanMember = BooleanMember("checked", required = false, default = false)
    val Label: StringMember = StringMember("label", required = false)
    val OnChange: ActionMember = ActionMember("on_change", required = false)
    val Enabled: BooleanMember = BooleanMember("enabled", required = false)
    val ThumbIcon: IdentifierMember = IdentifierMember("thumb_icon", required = false)
    override val required: List<NodeMember<*>> = listOf(Id)
    override val optional: List<NodeMember<*>> = listOf(Checked, Label, OnChange, Enabled, ThumbIcon)
}

/** Typed members of the `enum_list` node. */
object EnumListMembers : NodeMemberSet("enum_list") {
    val Id: IdentifierMember = IdentifierMember("id", required = true)
    val Options: ArrayMember = ArrayMember("options", required = true, tag = "enum-option-array")
    val Value: AnyMember = AnyMember("value", required = false, tag = "option-value-or-array")
    val MultiSelect: BooleanMember = BooleanMember("multi_select", required = false)
    val AllowAdd: BooleanMember = BooleanMember("allow_add", required = false)
    val OnChange: ActionMember = ActionMember("on_change", required = false)
    val Enabled: BooleanMember = BooleanMember("enabled", required = false)
    val Variant: EnumMember<EnumListVariant> = EnumMember("variant", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.EnumListVariant)
    val Children: NodeArrayMember = NodeArrayMember("children", required = false)
    override val required: List<NodeMember<*>> = listOf(Id, Options)
    override val optional: List<NodeMember<*>> = listOf(Value, MultiSelect, AllowAdd, OnChange, Enabled, Variant, Children)
}

/** Typed members of the `date_button` node. */
object DateButtonMembers : NodeMemberSet("date_button") {
    val Label: StringMember = StringMember("label", required = true)
    val OnPick: ActionMember = ActionMember("on_pick", required = true)
    val Value: StringMember = StringMember("value", required = false, tag = "yyyy-mm-dd", format = "yyyy-mm-dd")
    val Enabled: BooleanMember = BooleanMember("enabled", required = false)
    val Mode: EnumMember<DateButtonMode> = EnumMember("mode", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.DateButtonMode)
    val MinDate: StringMember = StringMember("min_date", required = false, tag = "yyyy-mm-dd", format = "yyyy-mm-dd")
    val MaxDate: StringMember = StringMember("max_date", required = false, tag = "yyyy-mm-dd", format = "yyyy-mm-dd")
    val DisabledWeekdays: ArrayMember = ArrayMember("disabled_weekdays", required = false, tag = "weekday-array")
    override val required: List<NodeMember<*>> = listOf(Label, OnPick)
    override val optional: List<NodeMember<*>> = listOf(Value, Enabled, Mode, MinDate, MaxDate, DisabledWeekdays)
}

/** Typed members of the `time_button` node. */
object TimeButtonMembers : NodeMemberSet("time_button") {
    val Label: StringMember = StringMember("label", required = true)
    val OnPick: ActionMember = ActionMember("on_pick", required = true)
    val Value: StringMember = StringMember("value", required = false, tag = "hh-mm", format = "hh-mm")
    val Enabled: BooleanMember = BooleanMember("enabled", required = false)
    val DisplayMode: EnumMember<TimeButtonDisplayMode> = EnumMember("display_mode", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.TimeButtonDisplayMode)
    override val required: List<NodeMember<*>> = listOf(Label, OnPick)
    override val optional: List<NodeMember<*>> = listOf(Value, Enabled, DisplayMode)
}

/** Typed members of the `slider` node. */
object SliderMembers : NodeMemberSet("slider") {
    val Id: IdentifierMember = IdentifierMember("id", required = true)
    val OnChange: ActionMember = ActionMember("on_change", required = true)
    val Value: NumberMember = NumberMember("value", required = false)
    val ValueEnd: NumberMember = NumberMember("value_end", required = false)
    val Min: NumberMember = NumberMember("min", required = false)
    val Max: NumberMember = NumberMember("max", required = false)
    val Values: NumberArrayMember = NumberArrayMember("values", required = false)
    val Enabled: BooleanMember = BooleanMember("enabled", required = false)
    val Track: EnumMember<SliderTrack> = EnumMember("track", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.SliderTrack)
    val Orientation: EnumMember<SliderOrientation> = EnumMember("orientation", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.SliderOrientation)
    val Color: ColorMember = ColorMember("color", required = false)
    val ColorEnd: ColorMember = ColorMember("color_end", required = false)
    val ThumbIcon: IdentifierMember = IdentifierMember("thumb_icon", required = false)
    val ValueLabel: BooleanMember = BooleanMember("value_label", required = false)
    val TrackIconStart: IdentifierMember = IdentifierMember("track_icon_start", required = false)
    val TrackIconEnd: IdentifierMember = IdentifierMember("track_icon_end", required = false)
    override val required: List<NodeMember<*>> = listOf(Id, OnChange)
    override val optional: List<NodeMember<*>> = listOf(Value, ValueEnd, Min, Max, Values, Enabled, Track, Orientation, Color, ColorEnd, ThumbIcon, ValueLabel, TrackIconStart, TrackIconEnd)
}

/** Typed members of the `chart` node. */
object ChartMembers : NodeMemberSet("chart") {
    val Series: ArrayMember = ArrayMember("series", required = true, tag = "chart-series-array")
    val Kind: EnumMember<ChartKind> = EnumMember("kind", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.ChartKind)
    val Height: NumberMember = NumberMember("height", required = false)
    val YRange: TwoNumberArrayMember = TwoNumberArrayMember("y_range", required = false)
    val Summary: StringMember = StringMember("summary", required = false)
    val OnPointTap: ActionMember = ActionMember("on_point_tap", required = false)
    val Children: NodeArrayMember = NodeArrayMember("children", required = false)
    override val required: List<NodeMember<*>> = listOf(Series)
    override val optional: List<NodeMember<*>> = listOf(Kind, Height, YRange, Summary, OnPointTap, Children)
}

/** Typed members of the `canvas` node. */
object CanvasMembers : NodeMemberSet("canvas") {
    val Width: NumberMember = NumberMember("width", required = true)
    val Height: NumberMember = NumberMember("height", required = true)
    val Ops: ArrayMember = ArrayMember("ops", required = true, tag = "canvas-op-array")
    val Children: NodeArrayMember = NodeArrayMember("children", required = false)
    override val required: List<NodeMember<*>> = listOf(Width, Height, Ops)
    override val optional: List<NodeMember<*>> = listOf(Children)
}

/** Typed members of the `month_grid` node. */
object MonthGridMembers : NodeMemberSet("month_grid") {
    val Month: StringMember = StringMember("month", required = true, tag = "yyyy-mm", format = "yyyy-mm")
    val Marks: ObjectMember = ObjectMember("marks", required = false, tag = "date-marks-object")
    val DayStyles: ObjectMember = ObjectMember("day_styles", required = false, tag = "date-style-object")
    val Selected: StringMember = StringMember("selected", required = false, tag = "yyyy-mm-dd", format = "yyyy-mm-dd")
    val MinMonth: StringMember = StringMember("min_month", required = false, tag = "yyyy-mm", format = "yyyy-mm")
    val MaxMonth: StringMember = StringMember("max_month", required = false, tag = "yyyy-mm", format = "yyyy-mm")
    val MinDate: StringMember = StringMember("min_date", required = false, tag = "yyyy-mm-dd", format = "yyyy-mm-dd")
    val MaxDate: StringMember = StringMember("max_date", required = false, tag = "yyyy-mm-dd", format = "yyyy-mm-dd")
    val DisabledWeekdays: ArrayMember = ArrayMember("disabled_weekdays", required = false, tag = "weekday-array")
    val RangeStart: StringMember = StringMember("range_start", required = false, tag = "yyyy-mm-dd", format = "yyyy-mm-dd")
    val RangeEnd: StringMember = StringMember("range_end", required = false, tag = "yyyy-mm-dd", format = "yyyy-mm-dd")
    val OnDayTap: ActionMember = ActionMember("on_day_tap", required = false)
    val OnMonthChange: ActionMember = ActionMember("on_month_change", required = false)
    val Children: NodeArrayMember = NodeArrayMember("children", required = false)
    override val required: List<NodeMember<*>> = listOf(Month)
    override val optional: List<NodeMember<*>> = listOf(Marks, DayStyles, Selected, MinMonth, MaxMonth, MinDate, MaxDate, DisabledWeekdays, RangeStart, RangeEnd, OnDayTap, OnMonthChange, Children)
}

/** Typed members of the `scaffold` node. */
object ScaffoldMembers : NodeMemberSet("scaffold") {
    val TopBar: ChildNodeMember = ChildNodeMember("top_bar", required = false)
    val Body: ChildNodeMember = ChildNodeMember("body", required = false)
    val BottomBar: ChildNodeMember = ChildNodeMember("bottom_bar", required = false)
    val Fab: ChildNodeMember = ChildNodeMember("fab", required = false)
    val FloatingToolbar: ChildNodeMember = ChildNodeMember("floating_toolbar", required = false)
    val Drawer: ChildNodeMember = ChildNodeMember("drawer", required = false)
    val Snackbar: StringMember = StringMember("snackbar", required = false)
    val SnackbarAction: ObjectMember = ObjectMember("snackbar_action", required = false, tag = "label-on-tap-object")
    val OnRefresh: ActionMember = ActionMember("on_refresh", required = false)
    val TopBarStyle: EnumMember<ScaffoldTopBarStyle> = EnumMember("top_bar_style", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.ScaffoldTopBarStyle)
    val TopBarSubtitle: StringMember = StringMember("top_bar_subtitle", required = false)
    val ScrollBehavior: EnumMember<ScaffoldScrollBehavior> = EnumMember("scroll_behavior", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.ScaffoldScrollBehavior)
    val FloatingToolbarOrientation: EnumMember<ScaffoldFloatingToolbarOrientation> = EnumMember("floating_toolbar_orientation", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.ScaffoldFloatingToolbarOrientation)
    val FloatingToolbarExpanded: BooleanMember = BooleanMember("floating_toolbar_expanded", required = false)
    val FloatingToolbarPlacement: EnumMember<ScaffoldFloatingToolbarPlacement> = EnumMember("floating_toolbar_placement", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.ScaffoldFloatingToolbarPlacement)
    val FloatingToolbarFab: ChildNodeMember = ChildNodeMember("floating_toolbar_fab", required = false)
    val FloatingToolbarScroll: BooleanMember = BooleanMember("floating_toolbar_scroll", required = false)
    val FloatingToolbarExitDirection: EnumMember<ScaffoldFloatingToolbarExitDirection> = EnumMember("floating_toolbar_exit_direction", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.ScaffoldFloatingToolbarExitDirection)
    val RefreshIndicator: EnumMember<ScaffoldRefreshIndicator> = EnumMember("refresh_indicator", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.ScaffoldRefreshIndicator)
    val IsRefreshing: BooleanMember = BooleanMember("is_refreshing", required = false)
    val SnackbarDuration: EnumMember<ScaffoldSnackbarDuration> = EnumMember("snackbar_duration", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.ScaffoldSnackbarDuration)
    val SnackbarDismiss: BooleanMember = BooleanMember("snackbar_dismiss", required = false)
    val SnackbarMaxLines: IntMember = IntMember("snackbar_max_lines", required = false, tag = "positive-integer", minimum = 1L, maximum = 9007199254740991L)
    val Sheet: ChildNodeMember = ChildNodeMember("sheet", required = false)
    val SheetPeekHeight: DpMember = DpMember("sheet_peek_height", required = false)
    val SheetState: EnumMember<ScaffoldSheetState> = EnumMember("sheet_state", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.ScaffoldSheetState)
    val OnSheetChange: ActionMember = ActionMember("on_sheet_change", required = false)
    val FabHideOnScroll: BooleanMember = BooleanMember("fab_hide_on_scroll", required = false)
    val DrawerVariant: EnumMember<ScaffoldDrawerVariant> = EnumMember("drawer_variant", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.ScaffoldDrawerVariant)
    val BottomBarBehavior: EnumMember<ScaffoldBottomBarBehavior> = EnumMember("bottom_bar_behavior", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.ScaffoldBottomBarBehavior)
    val FabPosition: EnumMember<ScaffoldFabPosition> = EnumMember("fab_position", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.ScaffoldFabPosition)
    val TopBarExpanded: ChildNodeMember = ChildNodeMember("top_bar_expanded", required = false)
    val TopBarCollapsedHeight: DpMember = DpMember("top_bar_collapsed_height", required = false)
    val TopBarExpandedHeight: DpMember = DpMember("top_bar_expanded_height", required = false)
    val TopBarCentered: BooleanMember = BooleanMember("top_bar_centered", required = false)
    val SnackbarContent: ChildNodeMember = ChildNodeMember("snackbar_content", required = false)
    val Rail: ChildNodeMember = ChildNodeMember("rail", required = false)
    override val required: List<NodeMember<*>> = listOf()
    override val optional: List<NodeMember<*>> = listOf(TopBar, Body, BottomBar, Fab, FloatingToolbar, Drawer, Snackbar, SnackbarAction, OnRefresh, TopBarStyle, TopBarSubtitle, ScrollBehavior, FloatingToolbarOrientation, FloatingToolbarExpanded, FloatingToolbarPlacement, FloatingToolbarFab, FloatingToolbarScroll, FloatingToolbarExitDirection, RefreshIndicator, IsRefreshing, SnackbarDuration, SnackbarDismiss, SnackbarMaxLines, Sheet, SheetPeekHeight, SheetState, OnSheetChange, FabHideOnScroll, DrawerVariant, BottomBarBehavior, FabPosition, TopBarExpanded, TopBarCollapsedHeight, TopBarExpandedHeight, TopBarCentered, SnackbarContent, Rail)
}

/** Typed members of the `tooltip` node. */
object TooltipMembers : NodeMemberSet("tooltip") {
    val Children: NodeArrayMember = NodeArrayMember("children", required = true)
    val Text: StringMember = StringMember("text", required = true)
    val Position: EnumMember<TooltipPosition> = EnumMember("position", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.TooltipPosition)
    val Caret: BooleanMember = BooleanMember("caret", required = false)
    val CaretWidth: DpMember = DpMember("caret_width", required = false)
    val CaretHeight: DpMember = DpMember("caret_height", required = false)
    val Rich: BooleanMember = BooleanMember("rich", required = false)
    val Title: StringMember = StringMember("title", required = false)
    val ActionLabel: StringMember = StringMember("action_label", required = false)
    val OnAction: ActionMember = ActionMember("on_action", required = false)
    val Shown: BooleanMember = BooleanMember("shown", required = false)
    override val required: List<NodeMember<*>> = listOf(Children, Text)
    override val optional: List<NodeMember<*>> = listOf(Position, Caret, CaretWidth, CaretHeight, Rich, Title, ActionLabel, OnAction, Shown)
}

/** Typed members of the `pane_scaffold` node. */
object PaneScaffoldMembers : NodeMemberSet("pane_scaffold") {
    val List: ChildNodeMember = ChildNodeMember("list", required = true)
    val Detail: ChildNodeMember = ChildNodeMember("detail", required = true)
    val Extra: ChildNodeMember = ChildNodeMember("extra", required = false)
    val Variant: EnumMember<PaneScaffoldVariant> = EnumMember("variant", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.PaneScaffoldVariant)
    override val required: List<NodeMember<*>> = listOf(List, Detail)
    override val optional: List<NodeMember<*>> = listOf(Extra, Variant)
}

/** Typed members of the `navigation_rail` node. */
object NavigationRailMembers : NodeMemberSet("navigation_rail") {
    val Items: ArrayMember = ArrayMember("items", required = true, tag = "rail-item-array")
    val Variant: EnumMember<NavigationRailVariant> = EnumMember("variant", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.NavigationRailVariant)
    val Expanded: BooleanOrAutoMember = BooleanOrAutoMember("expanded", required = false)
    val Arrangement: EnumMember<NavigationRailArrangement> = EnumMember("arrangement", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.NavigationRailArrangement)
    val Header: ChildNodeMember = ChildNodeMember("header", required = false)
    val HideOnCollapse: BooleanMember = BooleanMember("hide_on_collapse", required = false)
    val OnExpandChange: ActionMember = ActionMember("on_expand_change", required = false)
    override val required: List<NodeMember<*>> = listOf(Items)
    override val optional: List<NodeMember<*>> = listOf(Variant, Expanded, Arrangement, Header, HideOnCollapse, OnExpandChange)
}

/** Typed members of the `search_bar` node. */
object SearchBarMembers : NodeMemberSet("search_bar") {
    val Id: IdentifierMember = IdentifierMember("id", required = true)
    val Value: StringMember = StringMember("value", required = false)
    val Hint: StringMember = StringMember("hint", required = false)
    val Variant: EnumMember<SearchBarVariant> = EnumMember("variant", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.SearchBarVariant)
    val OnSearch: ActionMember = ActionMember("on_search", required = false)
    val OnChange: ActionMember = ActionMember("on_change", required = false)
    val LeadingIcon: IdentifierMember = IdentifierMember("leading_icon", required = false)
    val TrailingIcon: IdentifierMember = IdentifierMember("trailing_icon", required = false)
    val Children: NodeArrayMember = NodeArrayMember("children", required = false)
    val Enabled: BooleanMember = BooleanMember("enabled", required = false)
    override val required: List<NodeMember<*>> = listOf(Id)
    override val optional: List<NodeMember<*>> = listOf(Value, Hint, Variant, OnSearch, OnChange, LeadingIcon, TrailingIcon, Children, Enabled)
}

/** Typed members of the `dropdown` node. */
object DropdownMembers : NodeMemberSet("dropdown") {
    val Id: IdentifierMember = IdentifierMember("id", required = true)
    val Options: ArrayMember = ArrayMember("options", required = true, tag = "enum-option-array")
    val Value: ScalarMember = ScalarMember("value", required = false, tag = "option-value")
    val Label: StringMember = StringMember("label", required = false)
    val Hint: StringMember = StringMember("hint", required = false)
    val Editable: BooleanMember = BooleanMember("editable", required = false)
    val OnChange: ActionMember = ActionMember("on_change", required = false)
    val Enabled: BooleanMember = BooleanMember("enabled", required = false)
    val ReportCaret: BooleanMember = BooleanMember("report_caret", required = false)
    override val required: List<NodeMember<*>> = listOf(Id, Options)
    override val optional: List<NodeMember<*>> = listOf(Value, Label, Hint, Editable, OnChange, Enabled, ReportCaret)
}

/** Typed members of the `segmented_button` node. */
object SegmentedButtonMembers : NodeMemberSet("segmented_button") {
    val Id: IdentifierMember = IdentifierMember("id", required = true)
    val Options: ArrayMember = ArrayMember("options", required = true, tag = "enum-option-array")
    val Value: AnyMember = AnyMember("value", required = false, tag = "option-value-or-array")
    val MultiSelect: BooleanMember = BooleanMember("multi_select", required = false)
    val OnChange: ActionMember = ActionMember("on_change", required = false)
    val Enabled: BooleanMember = BooleanMember("enabled", required = false)
    override val required: List<NodeMember<*>> = listOf(Id, Options)
    override val optional: List<NodeMember<*>> = listOf(Value, MultiSelect, OnChange, Enabled)
}

/** Typed members of the `carousel` node. */
object CarouselMembers : NodeMemberSet("carousel") {
    val Children: NodeArrayMember = NodeArrayMember("children", required = true)
    val Strategy: EnumMember<CarouselStrategy> = EnumMember("strategy", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.CarouselStrategy)
    val ItemWidth: DpMember = DpMember("item_width", required = false)
    val ItemSpacing: DpMember = DpMember("item_spacing", required = false)
    val ContentPadding: DpMember = DpMember("content_padding", required = false)
    val ItemCorner: DpMember = DpMember("item_corner", required = false)
    override val required: List<NodeMember<*>> = listOf(Children)
    override val optional: List<NodeMember<*>> = listOf(Strategy, ItemWidth, ItemSpacing, ContentPadding, ItemCorner)
}

/** Typed members of the `button_group` node. */
object ButtonGroupMembers : NodeMemberSet("button_group") {
    val Items: ArrayMember = ArrayMember("items", required = true, tag = "button-group-item-array")
    val OverflowIcon: IdentifierMember = IdentifierMember("overflow_icon", required = false)
    override val required: List<NodeMember<*>> = listOf(Items)
    override val optional: List<NodeMember<*>> = listOf(OverflowIcon)
}

/** Typed members of the `lazy_grid` node. */
object LazyGridMembers : NodeMemberSet("lazy_grid") {
    val Children: NodeArrayMember = NodeArrayMember("children", required = true)
    val Columns: IntMember = IntMember("columns", required = false, tag = "positive-integer", minimum = 1L, maximum = 9007199254740991L)
    val MinItemWidth: DpMember = DpMember("min_item_width", required = false)
    val Reverse: BooleanMember = BooleanMember("reverse", required = false)
    val Spacing: DpMember = DpMember("spacing", required = false)
    val ContentPadding: DpMember = DpMember("content_padding", required = false)
    override val required: List<NodeMember<*>> = listOf(Children)
    override val optional: List<NodeMember<*>> = listOf(Columns, MinItemWidth, Reverse, Spacing, ContentPadding)
}

/** Universal node attributes (SPEC §16.5), readable on every node. */
object UniversalMembers : NodeMemberSet("*") {
    val Key: IdentifierMember = IdentifierMember("key", required = false)
    val Id: IdentifierMember = IdentifierMember("id", required = false)
    val ScrollHere: BooleanMember = BooleanMember("scroll_here", required = false)
    val Padding: DpMember = DpMember("padding", required = false)
    val Pad: ObjectMember = ObjectMember("pad", required = false, tag = "pad-object")
    val Width: NumberMember = NumberMember("width", required = false)
    val Height: NumberMember = NumberMember("height", required = false)
    val MinWidth: NumberMember = NumberMember("min_width", required = false)
    val MaxWidth: NumberMember = NumberMember("max_width", required = false)
    val MinHeight: NumberMember = NumberMember("min_height", required = false)
    val MaxHeight: NumberMember = NumberMember("max_height", required = false)
    val FillFraction: NumberMember = NumberMember("fill_fraction", required = false)
    val AspectRatio: NumberMember = NumberMember("aspect_ratio", required = false)
    val Weight: NumberMember = NumberMember("weight", required = false)
    val Bg: ColorMember = ColorMember("bg", required = false)
    val Corner: AnyMember = AnyMember("corner", required = false, tag = "number-or-corner-object")
    val Border: ObjectMember = ObjectMember("border", required = false, tag = "border-object")
    val Alpha: NumberMember = NumberMember("alpha", required = false)
    val Clip: BooleanMember = BooleanMember("clip", required = false)
    val AlignSelf: EnumMember<AlignSelf> = EnumMember("align_self", required = false, domain = com.calebc42.ebp.renderer.model.vocabulary.generated.AlignSelf)
    val Semantics: ObjectMember = ObjectMember("semantics", required = false, tag = "semantics-object")
    override val required: List<NodeMember<*>> = listOf()
    override val optional: List<NodeMember<*>> = listOf(Key, Id, ScrollHere, Padding, Pad, Width, Height, MinWidth, MaxWidth, MinHeight, MaxHeight, FillFraction, AspectRatio, Weight, Bg, Corner, Border, Alpha, Clip, AlignSelf, Semantics)
}

/** Every generated member set, keyed by node type. */
val CORE_NODE_MEMBERS: Map<String, NodeMemberSet> = mapOf(
    "text" to TextMembers,
    "rich_text" to RichTextMembers,
    "icon" to IconMembers,
    "image" to ImageMembers,
    "date_stamp" to DateStampMembers,
    "section_header" to SectionHeaderMembers,
    "empty_state" to EmptyStateMembers,
    "progress" to ProgressMembers,
    "badge" to BadgeMembers,
    "row" to RowMembers,
    "column" to ColumnMembers,
    "flow_row" to FlowRowMembers,
    "box" to BoxMembers,
    "surface" to SurfaceMembers,
    "lazy_column" to LazyColumnMembers,
    "variant_host" to VariantHostMembers,
    "spacer" to SpacerMembers,
    "divider" to DividerMembers,
    "card" to CardMembers,
    "collapsible" to CollapsibleMembers,
    "reorderable_list" to ReorderableListMembers,
    "tabs" to TabsMembers,
    "tab_selector" to TabSelectorMembers,
    "table" to TableMembers,
    "button" to ButtonMembers,
    "icon_button" to IconButtonMembers,
    "chip" to ChipMembers,
    "menu" to MenuMembers,
    "text_input" to TextInputMembers,
    "editor" to EditorMembers,
    "checkbox" to CheckboxMembers,
    "switch" to SwitchMembers,
    "enum_list" to EnumListMembers,
    "date_button" to DateButtonMembers,
    "time_button" to TimeButtonMembers,
    "slider" to SliderMembers,
    "chart" to ChartMembers,
    "canvas" to CanvasMembers,
    "month_grid" to MonthGridMembers,
    "scaffold" to ScaffoldMembers,
    "tooltip" to TooltipMembers,
    "pane_scaffold" to PaneScaffoldMembers,
    "navigation_rail" to NavigationRailMembers,
    "search_bar" to SearchBarMembers,
    "dropdown" to DropdownMembers,
    "segmented_button" to SegmentedButtonMembers,
    "carousel" to CarouselMembers,
    "button_group" to ButtonGroupMembers,
    "lazy_grid" to LazyGridMembers,
)
