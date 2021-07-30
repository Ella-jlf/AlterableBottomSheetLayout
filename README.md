для работы необходимы анимации

    implementation "androidx.dynamicanimation:dynamicanimation:1.0.0"

- `foreground` - ссылка на bg_some_background.xml(нужно для указания закругленных углов)
- `topCorners` -  Обрезание углов. 
- `background_color` - цвет заднего фона
- `margin_top` - отступ сверху
- `hide_on_background_click` - скрывать по нажатию на задний фон
- `isDraggable` -будет ли доступно перетаскивание view.
- `foregoround_type` - скрывающийся тип.
- `intermediate_height` - высота промежуточной остановки
- `foreground_heigh` - возможность задавать height для foreground.(wrap_content, match_parent - доступны)

_____________________________________________
- `show()` - показывает bottom sheet
- `hide()` - скрывает его