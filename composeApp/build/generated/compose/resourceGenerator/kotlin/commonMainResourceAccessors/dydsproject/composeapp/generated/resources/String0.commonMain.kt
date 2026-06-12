@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package dydsproject.composeapp.generated.resources

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.StringResource

private object CommonMainString0 {
  public val app_name: StringResource by 
      lazy { init_app_name() }

  public val error: StringResource by 
      lazy { init_error() }

  public val original_language: StringResource by 
      lazy { init_original_language() }

  public val original_title: StringResource by 
      lazy { init_original_title() }

  public val popularity: StringResource by 
      lazy { init_popularity() }

  public val release_date: StringResource by 
      lazy { init_release_date() }

  public val vote_average: StringResource by 
      lazy { init_vote_average() }
}

@InternalResourceApi
internal fun _collectCommonMainString0Resources(map: MutableMap<String, StringResource>) {
  map.put("app_name", CommonMainString0.app_name)
  map.put("error", CommonMainString0.error)
  map.put("original_language", CommonMainString0.original_language)
  map.put("original_title", CommonMainString0.original_title)
  map.put("popularity", CommonMainString0.popularity)
  map.put("release_date", CommonMainString0.release_date)
  map.put("vote_average", CommonMainString0.vote_average)
}

internal val Res.string.app_name: StringResource
  get() = CommonMainString0.app_name

private fun init_app_name(): StringResource = org.jetbrains.compose.resources.StringResource(
  "string:app_name", "app_name",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/dydsproject.composeapp.generated.resources/values/strings.commonMain.cvr", 10,
    28),
    )
)

internal val Res.string.error: StringResource
  get() = CommonMainString0.error

private fun init_error(): StringResource = org.jetbrains.compose.resources.StringResource(
  "string:error", "error",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/dydsproject.composeapp.generated.resources/values/strings.commonMain.cvr", 39,
    21),
    )
)

internal val Res.string.original_language: StringResource
  get() = CommonMainString0.original_language

private fun init_original_language(): StringResource =
    org.jetbrains.compose.resources.StringResource(
  "string:original_language", "original_language",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/dydsproject.composeapp.generated.resources/values/strings.commonMain.cvr", 61,
    49),
    )
)

internal val Res.string.original_title: StringResource
  get() = CommonMainString0.original_title

private fun init_original_title(): StringResource = org.jetbrains.compose.resources.StringResource(
  "string:original_title", "original_title",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/dydsproject.composeapp.generated.resources/values/strings.commonMain.cvr",
    111, 42),
    )
)

internal val Res.string.popularity: StringResource
  get() = CommonMainString0.popularity

private fun init_popularity(): StringResource = org.jetbrains.compose.resources.StringResource(
  "string:popularity", "popularity",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/dydsproject.composeapp.generated.resources/values/strings.commonMain.cvr",
    154, 34),
    )
)

internal val Res.string.release_date: StringResource
  get() = CommonMainString0.release_date

private fun init_release_date(): StringResource = org.jetbrains.compose.resources.StringResource(
  "string:release_date", "release_date",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/dydsproject.composeapp.generated.resources/values/strings.commonMain.cvr",
    189, 36),
    )
)

internal val Res.string.vote_average: StringResource
  get() = CommonMainString0.vote_average

private fun init_vote_average(): StringResource = org.jetbrains.compose.resources.StringResource(
  "string:vote_average", "vote_average",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/dydsproject.composeapp.generated.resources/values/strings.commonMain.cvr",
    226, 36),
    )
)
