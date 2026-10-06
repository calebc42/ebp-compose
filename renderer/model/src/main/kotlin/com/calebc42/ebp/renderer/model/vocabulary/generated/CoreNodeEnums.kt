// SPDX-License-Identifier: GPL-3.0-or-later
// GENERATED from ebp/contract.json (contract_format 12, spec 3.1.0-draft) by
// tools/gen-node-members.py. DO NOT EDIT: CoreNodeMembersDriftTest pins it.
package com.calebc42.ebp.renderer.model.vocabulary.generated

import com.calebc42.ebp.renderer.model.vocabulary.EnumDomain
import com.calebc42.ebp.renderer.model.vocabulary.WireEnum

/** Contract enum domain `text.style`. */
enum class TextStyle(override val wire: String) : WireEnum {
    Body("body"),
    Title("title"),
    Headline("headline"),
    Caption("caption"),
    Label("label"),
    Mono("mono");

    companion object : EnumDomain<TextStyle> {
        override val contractKey: String = "text.style"
        override val values: List<TextStyle> get() = entries
    }
}

/** Contract enum domain `rich_text.style`. */
enum class RichTextStyle(override val wire: String) : WireEnum {
    Body("body"),
    Title("title"),
    Headline("headline"),
    Caption("caption"),
    Label("label"),
    Mono("mono");

    companion object : EnumDomain<RichTextStyle> {
        override val contractKey: String = "rich_text.style"
        override val values: List<RichTextStyle> get() = entries
    }
}

/** Contract enum domain `progress.variant`. */
enum class ProgressVariant(override val wire: String) : WireEnum {
    Circular("circular"),
    Linear("linear"),
    LinearWavy("linear_wavy"),
    CircularWavy("circular_wavy"),
    Loading("loading"),
    ContainedLoading("contained_loading");

    companion object : EnumDomain<ProgressVariant> {
        override val contractKey: String = "progress.variant"
        override val values: List<ProgressVariant> get() = entries
    }
}

/** Contract enum domain `button.variant`. */
enum class ButtonVariant(override val wire: String) : WireEnum {
    Filled("filled"),
    Tonal("tonal"),
    Elevated("elevated"),
    Outlined("outlined"),
    Text("text");

    companion object : EnumDomain<ButtonVariant> {
        override val contractKey: String = "button.variant"
        override val values: List<ButtonVariant> get() = entries
    }
}

/** Contract enum domain `image.content_scale`. */
enum class ImageContentScale(override val wire: String) : WireEnum {
    Fit("fit"),
    Crop("crop"),
    Fill("fill");

    companion object : EnumDomain<ImageContentScale> {
        override val contractKey: String = "image.content_scale"
        override val values: List<ImageContentScale> get() = entries
    }
}

/** Contract enum domain `row.align`. */
enum class RowAlign(override val wire: String) : WireEnum {
    Top("top"),
    Center("center"),
    Bottom("bottom"),
    Baseline("baseline");

    companion object : EnumDomain<RowAlign> {
        override val contractKey: String = "row.align"
        override val values: List<RowAlign> get() = entries
    }
}

/** Contract enum domain `column.align`. */
enum class ColumnAlign(override val wire: String) : WireEnum {
    Start("start"),
    Center("center"),
    End("end");

    companion object : EnumDomain<ColumnAlign> {
        override val contractKey: String = "column.align"
        override val values: List<ColumnAlign> get() = entries
    }
}

/** Contract enum domain `flow_row.align`. */
enum class FlowRowAlign(override val wire: String) : WireEnum {
    Top("top"),
    Center("center"),
    Bottom("bottom");

    companion object : EnumDomain<FlowRowAlign> {
        override val contractKey: String = "flow_row.align"
        override val values: List<FlowRowAlign> get() = entries
    }
}

/** Contract enum domain `arrange`. */
enum class Arrange(override val wire: String) : WireEnum {
    Start("start"),
    Center("center"),
    End("end"),
    SpaceBetween("space_between"),
    SpaceAround("space_around"),
    SpaceEvenly("space_evenly");

    companion object : EnumDomain<Arrange> {
        override val contractKey: String = "arrange"
        override val values: List<Arrange> get() = entries
    }
}

/** Contract enum domain `box.alignment`. */
enum class BoxAlignment(override val wire: String) : WireEnum {
    TopStart("top_start"),
    TopCenter("top_center"),
    TopEnd("top_end"),
    CenterStart("center_start"),
    Center("center"),
    CenterEnd("center_end"),
    BottomStart("bottom_start"),
    BottomCenter("bottom_center"),
    BottomEnd("bottom_end");

    companion object : EnumDomain<BoxAlignment> {
        override val contractKey: String = "box.alignment"
        override val values: List<BoxAlignment> get() = entries
    }
}

/** Contract enum domain `surface.shape`. */
enum class SurfaceShape(override val wire: String) : WireEnum {
    Rounded("rounded"),
    RoundedSmall("rounded_small"),
    Circle("circle"),
    Square("square"),
    Slanted("slanted"),
    Arch("arch"),
    Fan("fan"),
    Arrow("arrow"),
    SemiCircle("semi_circle"),
    Oval("oval"),
    Pill("pill"),
    Triangle("triangle"),
    Diamond("diamond"),
    ClamShell("clam_shell"),
    Pentagon("pentagon"),
    Gem("gem"),
    Sunny("sunny"),
    VerySunny("very_sunny"),
    Cookie4Sided("cookie_4_sided"),
    Cookie6Sided("cookie_6_sided"),
    Cookie7Sided("cookie_7_sided"),
    Cookie9Sided("cookie_9_sided"),
    Cookie12Sided("cookie_12_sided"),
    Ghostish("ghostish"),
    Clover4Leaf("clover_4_leaf"),
    Clover8Leaf("clover_8_leaf"),
    Burst("burst"),
    SoftBurst("soft_burst"),
    Boom("boom"),
    SoftBoom("soft_boom"),
    Flower("flower"),
    Puffy("puffy"),
    PuffyDiamond("puffy_diamond"),
    PixelCircle("pixel_circle"),
    PixelTriangle("pixel_triangle"),
    Bun("bun"),
    Heart("heart");

    companion object : EnumDomain<SurfaceShape> {
        override val contractKey: String = "surface.shape"
        override val values: List<SurfaceShape> get() = entries
    }
}

/** Contract enum domain `chart.kind`. */
enum class ChartKind(override val wire: String) : WireEnum {
    Line("line"),
    Bar("bar"),
    Area("area"),
    Sparkline("sparkline");

    companion object : EnumDomain<ChartKind> {
        override val contractKey: String = "chart.kind"
        override val values: List<ChartKind> get() = entries
    }
}

/** Contract enum domain `text_input.keyboard`. */
enum class TextInputKeyboard(override val wire: String) : WireEnum {
    Text("text"),
    Number("number"),
    Decimal("decimal"),
    Email("email"),
    Phone("phone"),
    Uri("uri");

    companion object : EnumDomain<TextInputKeyboard> {
        override val contractKey: String = "text_input.keyboard"
        override val values: List<TextInputKeyboard> get() = entries
    }
}

/** Contract enum domain `align_self`. */
enum class AlignSelf(override val wire: String) : WireEnum {
    Start("start"),
    Center("center"),
    End("end"),
    Stretch("stretch");

    companion object : EnumDomain<AlignSelf> {
        override val contractKey: String = "align_self"
        override val values: List<AlignSelf> get() = entries
    }
}

/** Contract enum domain `button.size`. */
enum class ButtonSize(override val wire: String) : WireEnum {
    Xsmall("xsmall"),
    Small("small"),
    Medium("medium"),
    Large("large"),
    Xlarge("xlarge");

    companion object : EnumDomain<ButtonSize> {
        override val contractKey: String = "button.size"
        override val values: List<ButtonSize> get() = entries
    }
}

/** Contract enum domain `button.shape`. */
enum class ButtonShape(override val wire: String) : WireEnum {
    Round("round"),
    Square("square");

    companion object : EnumDomain<ButtonShape> {
        override val contractKey: String = "button.shape"
        override val values: List<ButtonShape> get() = entries
    }
}

/** Contract enum domain `icon_button.variant`. */
enum class IconButtonVariant(override val wire: String) : WireEnum {
    Filled("filled"),
    Tonal("tonal"),
    Outlined("outlined");

    companion object : EnumDomain<IconButtonVariant> {
        override val contractKey: String = "icon_button.variant"
        override val values: List<IconButtonVariant> get() = entries
    }
}

/** Contract enum domain `card.variant`. */
enum class CardVariant(override val wire: String) : WireEnum {
    Filled("filled"),
    Elevated("elevated"),
    Outlined("outlined");

    companion object : EnumDomain<CardVariant> {
        override val contractKey: String = "card.variant"
        override val values: List<CardVariant> get() = entries
    }
}

/** Contract enum domain `chip.variant`. */
enum class ChipVariant(override val wire: String) : WireEnum {
    Flat("flat"),
    Elevated("elevated"),
    Input("input");

    companion object : EnumDomain<ChipVariant> {
        override val contractKey: String = "chip.variant"
        override val values: List<ChipVariant> get() = entries
    }
}

/** Contract enum domain `text_input.variant`. */
enum class TextInputVariant(override val wire: String) : WireEnum {
    Outlined("outlined"),
    Filled("filled");

    companion object : EnumDomain<TextInputVariant> {
        override val contractKey: String = "text_input.variant"
        override val values: List<TextInputVariant> get() = entries
    }
}

/** Contract enum domain `slider.track`. */
enum class SliderTrack(override val wire: String) : WireEnum {
    Default("default"),
    Centered("centered");

    companion object : EnumDomain<SliderTrack> {
        override val contractKey: String = "slider.track"
        override val values: List<SliderTrack> get() = entries
    }
}

/** Contract enum domain `slider.orientation`. */
enum class SliderOrientation(override val wire: String) : WireEnum {
    Horizontal("horizontal"),
    Vertical("vertical");

    companion object : EnumDomain<SliderOrientation> {
        override val contractKey: String = "slider.orientation"
        override val values: List<SliderOrientation> get() = entries
    }
}

/** Contract enum domain `checkbox.state`. */
enum class CheckboxState(override val wire: String) : WireEnum {
    Off("off"),
    On("on"),
    Indeterminate("indeterminate");

    companion object : EnumDomain<CheckboxState> {
        override val contractKey: String = "checkbox.state"
        override val values: List<CheckboxState> get() = entries
    }
}

/** Contract enum domain `text_input.filter`. */
enum class TextInputFilter(override val wire: String) : WireEnum {
    Digits("digits"),
    Alnum("alnum");

    companion object : EnumDomain<TextInputFilter> {
        override val contractKey: String = "text_input.filter"
        override val values: List<TextInputFilter> get() = entries
    }
}

/** Contract enum domain `menu.initial_scroll`. */
enum class MenuInitialScroll(override val wire: String) : WireEnum {
    Start("start"),
    End("end");

    companion object : EnumDomain<MenuInitialScroll> {
        override val contractKey: String = "menu.initial_scroll"
        override val values: List<MenuInitialScroll> get() = entries
    }
}

/** Contract enum domain `date_button.mode`. */
enum class DateButtonMode(override val wire: String) : WireEnum {
    Calendar("calendar"),
    Input("input");

    companion object : EnumDomain<DateButtonMode> {
        override val contractKey: String = "date_button.mode"
        override val values: List<DateButtonMode> get() = entries
    }
}

/** Contract enum domain `time_button.display_mode`. */
enum class TimeButtonDisplayMode(override val wire: String) : WireEnum {
    Picker("picker"),
    Input("input"),
    Switchable("switchable");

    companion object : EnumDomain<TimeButtonDisplayMode> {
        override val contractKey: String = "time_button.display_mode"
        override val values: List<TimeButtonDisplayMode> get() = entries
    }
}

/** Contract enum domain `tooltip.position`. */
enum class TooltipPosition(override val wire: String) : WireEnum {
    Above("above"),
    Below("below"),
    Left("left"),
    Right("right"),
    Start("start"),
    End("end");

    companion object : EnumDomain<TooltipPosition> {
        override val contractKey: String = "tooltip.position"
        override val values: List<TooltipPosition> get() = entries
    }
}

/** Contract enum domain `icon_button.size`. */
enum class IconButtonSize(override val wire: String) : WireEnum {
    Xsmall("xsmall"),
    Small("small"),
    Medium("medium"),
    Large("large");

    companion object : EnumDomain<IconButtonSize> {
        override val contractKey: String = "icon_button.size"
        override val values: List<IconButtonSize> get() = entries
    }
}

/** Contract enum domain `icon_button.shape`. */
enum class IconButtonShape(override val wire: String) : WireEnum {
    Round("round"),
    Square("square");

    companion object : EnumDomain<IconButtonShape> {
        override val contractKey: String = "icon_button.shape"
        override val values: List<IconButtonShape> get() = entries
    }
}

/** Contract enum domain `icon_button.width_mode`. */
enum class IconButtonWidthMode(override val wire: String) : WireEnum {
    Narrow("narrow"),
    Uniform("uniform"),
    Wide("wide");

    companion object : EnumDomain<IconButtonWidthMode> {
        override val contractKey: String = "icon_button.width_mode"
        override val values: List<IconButtonWidthMode> get() = entries
    }
}

/** Contract enum domain `scaffold.top_bar_style`. */
enum class ScaffoldTopBarStyle(override val wire: String) : WireEnum {
    Small("small"),
    Center("center"),
    Medium("medium"),
    Large("large"),
    MediumFlexible("medium_flexible"),
    LargeFlexible("large_flexible"),
    TwoRows("two_rows");

    companion object : EnumDomain<ScaffoldTopBarStyle> {
        override val contractKey: String = "scaffold.top_bar_style"
        override val values: List<ScaffoldTopBarStyle> get() = entries
    }
}

/** Contract enum domain `scaffold.scroll_behavior`. */
enum class ScaffoldScrollBehavior(override val wire: String) : WireEnum {
    Pinned("pinned"),
    EnterAlways("enter_always"),
    ExitUntilCollapsed("exit_until_collapsed");

    companion object : EnumDomain<ScaffoldScrollBehavior> {
        override val contractKey: String = "scaffold.scroll_behavior"
        override val values: List<ScaffoldScrollBehavior> get() = entries
    }
}

/** Contract enum domain `scaffold.refresh_indicator`. */
enum class ScaffoldRefreshIndicator(override val wire: String) : WireEnum {
    Default("default"),
    Loading("loading"),
    None("none");

    companion object : EnumDomain<ScaffoldRefreshIndicator> {
        override val contractKey: String = "scaffold.refresh_indicator"
        override val values: List<ScaffoldRefreshIndicator> get() = entries
    }
}

/** Contract enum domain `scaffold.snackbar_duration`. */
enum class ScaffoldSnackbarDuration(override val wire: String) : WireEnum {
    Short("short"),
    Long("long"),
    Indefinite("indefinite");

    companion object : EnumDomain<ScaffoldSnackbarDuration> {
        override val contractKey: String = "scaffold.snackbar_duration"
        override val values: List<ScaffoldSnackbarDuration> get() = entries
    }
}

/** Contract enum domain `enum_list.variant`. */
enum class EnumListVariant(override val wire: String) : WireEnum {
    Chips("chips"),
    Radio("radio");

    companion object : EnumDomain<EnumListVariant> {
        override val contractKey: String = "enum_list.variant"
        override val values: List<EnumListVariant> get() = entries
    }
}

/** Contract enum domain `scaffold.sheet_state`. */
enum class ScaffoldSheetState(override val wire: String) : WireEnum {
    Hidden("hidden"),
    Partial("partial"),
    Expanded("expanded");

    companion object : EnumDomain<ScaffoldSheetState> {
        override val contractKey: String = "scaffold.sheet_state"
        override val values: List<ScaffoldSheetState> get() = entries
    }
}

/** Contract enum domain `tabs.style`. */
enum class TabsStyle(override val wire: String) : WireEnum {
    Primary("primary"),
    Secondary("secondary");

    companion object : EnumDomain<TabsStyle> {
        override val contractKey: String = "tabs.style"
        override val values: List<TabsStyle> get() = entries
    }
}

/** Contract enum domain `tab_selector.style`. */
enum class TabSelectorStyle(override val wire: String) : WireEnum {
    Primary("primary"),
    Secondary("secondary");

    companion object : EnumDomain<TabSelectorStyle> {
        override val contractKey: String = "tab_selector.style"
        override val values: List<TabSelectorStyle> get() = entries
    }
}

/** Contract enum domain `scaffold.floating_toolbar_orientation`. */
enum class ScaffoldFloatingToolbarOrientation(override val wire: String) : WireEnum {
    Horizontal("horizontal"),
    Vertical("vertical");

    companion object : EnumDomain<ScaffoldFloatingToolbarOrientation> {
        override val contractKey: String = "scaffold.floating_toolbar_orientation"
        override val values: List<ScaffoldFloatingToolbarOrientation> get() = entries
    }
}

/** Contract enum domain `scaffold.floating_toolbar_placement`. */
enum class ScaffoldFloatingToolbarPlacement(override val wire: String) : WireEnum {
    BottomCenter("bottom_center"),
    BottomStart("bottom_start"),
    BottomEnd("bottom_end"),
    CenterStart("center_start"),
    CenterEnd("center_end");

    companion object : EnumDomain<ScaffoldFloatingToolbarPlacement> {
        override val contractKey: String = "scaffold.floating_toolbar_placement"
        override val values: List<ScaffoldFloatingToolbarPlacement> get() = entries
    }
}

/** Contract enum domain `scaffold.floating_toolbar_exit_direction`. */
enum class ScaffoldFloatingToolbarExitDirection(override val wire: String) : WireEnum {
    Bottom("bottom"),
    Top("top"),
    Start("start"),
    End("end");

    companion object : EnumDomain<ScaffoldFloatingToolbarExitDirection> {
        override val contractKey: String = "scaffold.floating_toolbar_exit_direction"
        override val values: List<ScaffoldFloatingToolbarExitDirection> get() = entries
    }
}

/** Contract enum domain `pane_scaffold.variant`. */
enum class PaneScaffoldVariant(override val wire: String) : WireEnum {
    ListDetail("list_detail"),
    Supporting("supporting");

    companion object : EnumDomain<PaneScaffoldVariant> {
        override val contractKey: String = "pane_scaffold.variant"
        override val values: List<PaneScaffoldVariant> get() = entries
    }
}

/** Contract enum domain `navigation_rail.variant`. */
enum class NavigationRailVariant(override val wire: String) : WireEnum {
    Standard("standard"),
    Wide("wide"),
    Modal("modal");

    companion object : EnumDomain<NavigationRailVariant> {
        override val contractKey: String = "navigation_rail.variant"
        override val values: List<NavigationRailVariant> get() = entries
    }
}

/** Contract enum domain `scaffold.drawer_variant`. */
enum class ScaffoldDrawerVariant(override val wire: String) : WireEnum {
    Modal("modal"),
    Dismissible("dismissible"),
    Permanent("permanent");

    companion object : EnumDomain<ScaffoldDrawerVariant> {
        override val contractKey: String = "scaffold.drawer_variant"
        override val values: List<ScaffoldDrawerVariant> get() = entries
    }
}

/** Contract enum domain `scaffold.bottom_bar_behavior`. */
enum class ScaffoldBottomBarBehavior(override val wire: String) : WireEnum {
    Pinned("pinned"),
    ExitAlways("exit_always");

    companion object : EnumDomain<ScaffoldBottomBarBehavior> {
        override val contractKey: String = "scaffold.bottom_bar_behavior"
        override val values: List<ScaffoldBottomBarBehavior> get() = entries
    }
}

/** Contract enum domain `scaffold.fab_position`. */
enum class ScaffoldFabPosition(override val wire: String) : WireEnum {
    End("end"),
    EndOverlay("end_overlay"),
    Center("center");

    companion object : EnumDomain<ScaffoldFabPosition> {
        override val contractKey: String = "scaffold.fab_position"
        override val values: List<ScaffoldFabPosition> get() = entries
    }
}

/** Contract enum domain `button.checked_shape`. */
enum class ButtonCheckedShape(override val wire: String) : WireEnum {
    Round("round"),
    Square("square");

    companion object : EnumDomain<ButtonCheckedShape> {
        override val contractKey: String = "button.checked_shape"
        override val values: List<ButtonCheckedShape> get() = entries
    }
}

/** Contract enum domain `navigation_rail.arrangement`. */
enum class NavigationRailArrangement(override val wire: String) : WireEnum {
    Top("top"),
    Center("center"),
    Bottom("bottom");

    companion object : EnumDomain<NavigationRailArrangement> {
        override val contractKey: String = "navigation_rail.arrangement"
        override val values: List<NavigationRailArrangement> get() = entries
    }
}

/** Contract enum domain `carousel.strategy`. */
enum class CarouselStrategy(override val wire: String) : WireEnum {
    MultiBrowse("multi_browse"),
    Uncontained("uncontained"),
    CenteredHero("centered_hero");

    companion object : EnumDomain<CarouselStrategy> {
        override val contractKey: String = "carousel.strategy"
        override val values: List<CarouselStrategy> get() = entries
    }
}

/** Contract enum domain `search_bar.variant`. */
enum class SearchBarVariant(override val wire: String) : WireEnum {
    FullScreen("full_screen"),
    Docked("docked");

    companion object : EnumDomain<SearchBarVariant> {
        override val contractKey: String = "search_bar.variant"
        override val values: List<SearchBarVariant> get() = entries
    }
}

/** Contract enum domain `button.shape_role`. */
enum class ButtonShapeRole(override val wire: String) : WireEnum {
    Leading("leading"),
    Middle("middle"),
    Trailing("trailing"),
    Top("top"),
    Bottom("bottom");

    companion object : EnumDomain<ButtonShapeRole> {
        override val contractKey: String = "button.shape_role"
        override val values: List<ButtonShapeRole> get() = entries
    }
}

/** Every generated enum domain, keyed by its contract key. */
val CORE_NODE_ENUMS: Map<String, EnumDomain<*>> = mapOf(
    "text.style" to TextStyle,
    "rich_text.style" to RichTextStyle,
    "progress.variant" to ProgressVariant,
    "button.variant" to ButtonVariant,
    "image.content_scale" to ImageContentScale,
    "row.align" to RowAlign,
    "column.align" to ColumnAlign,
    "flow_row.align" to FlowRowAlign,
    "arrange" to Arrange,
    "box.alignment" to BoxAlignment,
    "surface.shape" to SurfaceShape,
    "chart.kind" to ChartKind,
    "text_input.keyboard" to TextInputKeyboard,
    "align_self" to AlignSelf,
    "button.size" to ButtonSize,
    "button.shape" to ButtonShape,
    "icon_button.variant" to IconButtonVariant,
    "card.variant" to CardVariant,
    "chip.variant" to ChipVariant,
    "text_input.variant" to TextInputVariant,
    "slider.track" to SliderTrack,
    "slider.orientation" to SliderOrientation,
    "checkbox.state" to CheckboxState,
    "text_input.filter" to TextInputFilter,
    "menu.initial_scroll" to MenuInitialScroll,
    "date_button.mode" to DateButtonMode,
    "time_button.display_mode" to TimeButtonDisplayMode,
    "tooltip.position" to TooltipPosition,
    "icon_button.size" to IconButtonSize,
    "icon_button.shape" to IconButtonShape,
    "icon_button.width_mode" to IconButtonWidthMode,
    "scaffold.top_bar_style" to ScaffoldTopBarStyle,
    "scaffold.scroll_behavior" to ScaffoldScrollBehavior,
    "scaffold.refresh_indicator" to ScaffoldRefreshIndicator,
    "scaffold.snackbar_duration" to ScaffoldSnackbarDuration,
    "enum_list.variant" to EnumListVariant,
    "scaffold.sheet_state" to ScaffoldSheetState,
    "tabs.style" to TabsStyle,
    "tab_selector.style" to TabSelectorStyle,
    "scaffold.floating_toolbar_orientation" to ScaffoldFloatingToolbarOrientation,
    "scaffold.floating_toolbar_placement" to ScaffoldFloatingToolbarPlacement,
    "scaffold.floating_toolbar_exit_direction" to ScaffoldFloatingToolbarExitDirection,
    "pane_scaffold.variant" to PaneScaffoldVariant,
    "navigation_rail.variant" to NavigationRailVariant,
    "scaffold.drawer_variant" to ScaffoldDrawerVariant,
    "scaffold.bottom_bar_behavior" to ScaffoldBottomBarBehavior,
    "scaffold.fab_position" to ScaffoldFabPosition,
    "button.checked_shape" to ButtonCheckedShape,
    "navigation_rail.arrangement" to NavigationRailArrangement,
    "carousel.strategy" to CarouselStrategy,
    "search_bar.variant" to SearchBarVariant,
    "button.shape_role" to ButtonShapeRole,
)
