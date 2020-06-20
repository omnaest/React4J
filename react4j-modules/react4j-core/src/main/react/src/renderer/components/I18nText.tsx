

export interface I18nTextValue extends ObjectMap<string>
{
}

export interface ObjectMap<E>
{
    [property: string]: E;
}

export class I18nRenderer
{
    public static render(value: I18nTextValue): string
    {
        return value ? value["DEFAULT"] : "";
    }
}