для работы необходимы анимации

    implementation "androidx.dynamicanimation:dynamicanimation:1.0.0"

- `foreground` - ссылка на bg_some_background.xml(нужно для указания закругленных углов)
- `head_layout` - дефолтный layout(frame=default,linear,relative). Если требуется иной, можно просто вложить в этот bottom sheet любой layout через xml. 
- `transparency_percent` - от 0.0 до 1.0 процент прозрачности
- `background_color` - цвет заднего фона
- `foreground_color` - цвет переднего фона(или указать сразу при передаче в foreground)-
- `margin_top` - отступ сверху
- `hide_on_background_click` - скрывать по нажатию на задний фон
- `isHidable` - определяет можно ли вообще её скрыть scroll-ом или по нажатию на background(если доступно), при false требуется скрывать ручками через метод hide()
- `allow_limited_area_span` - нужен для случаев, когда контент перекрывает все свободные поля у bottom sheet, и требуется draggable возможность. Важно, draggable работает в любом месте bottom sheet, если этот клик не обрабатывается больше никем(clild.onTouch() = false).
- `limited_area_span` - ширина области, по нажатию на которую будет доступна draggable bottom sheet-а, считается от верхней точки foreground и вниз на указанное dp. Идет в паре с `allow_limited_area_span`
- `foreground_heigh` - возможность задавать height для foreground.(wrap_content, match_parent - доступны)

_____________________________________________
- `show()` - показывает bottom sheet
- `hide()` - скрывает его