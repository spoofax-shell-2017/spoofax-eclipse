[
   Views                                  -- V  [H  [KW["views"]] _1],
   Views.1:iter-star                      -- _1,
   OutlineView                            -- KW["outline"] KW["view"] KW[":"] _1 _2 _3,
   OutlineView.2:iter-star                -- _1,
   OutlineView.3:opt                      -- _1,
   Source                                 -- KW["("] KW["source"] KW[")"],
   OnSelection                            -- KW["("] KW["onselection"] KW[")"],
   ExpandToLevel                          -- KW["expand"] KW["to"] KW["level"] KW[":"] _1,
   PropertiesView                         -- KW["properties"] KW["view"] KW[":"] _1 _2,
   PropertiesView.2:iter-star             -- _1,
   Source                                 -- KW["("] KW["source"] KW[")"],
   COMPLETION-Section                     -- _1,
   COMPLETION-OutlineOption               -- _1,
   COMPLETION-ExpandToLevel               -- _1,
   COMPLETION-View                        -- _1,
   COMPLETION-PropertiesOption            -- _1,
   Menus                                  -- V  [H  [KW["menus"]] _1],
   Menus.1:iter-star                      -- _1,
   ToolbarMenu                            -- KW["menu"] KW[":"] _1 _2 _3,
   ToolbarMenu.2:iter-star                -- _1,
   ToolbarMenu.3:iter-star                -- _1,
   Action                                 -- KW["action"] KW[":"] _1 KW["="] _2 _3,
   Action.3:iter-star                     -- _1,
   Submenu                                -- KW["submenu"] KW[":"] _1 _2 _3 KW["end"],
   Submenu.2:iter-star                    -- _1,
   Submenu.3:iter-star                    -- _1,
   Separator                              -- KW["separator"],
   Label                                  -- _1,
   Icon                                   -- KW["Icon"] KW["("] _1 KW[")"],
   OpenEditor                             -- KW["("] KW["openeditor"] KW[")"],
   RealTime                               -- KW["("] KW["realtime"] KW[")"],
   Meta                                   -- KW["("] KW["meta"] KW[")"],
   Source                                 -- KW["("] KW["source"] KW[")"],
   COMPLETION-Section                     -- _1,
   COMPLETION-Menu                        -- _1,
   COMPLETION-MenuContrib                 -- _1,
   COMPLETION-MenuID                      -- _1,
   COMPLETION-MenusOption                 -- _1,
   CompletionTemplateEx                   -- KW["completion"] KW["template"] _1 KW[":"] _2 _3 _4,
   CompletionTemplateEx.1:iter-star       -- _1,
   CompletionTemplateEx.4:iter-star       -- _1,
   KeyCombination                         -- _1,
   SemanticProvider                       -- KW["provider"] _1,
   IdentifierLexical                      -- KW["completion"] KW["lexical"] KW[":"] _1,
   CompletionTemplate                     -- KW["completion"] KW["template"] KW[":"] _1 _2 _3,
   CompletionTemplate.2:iter              -- _1,
   CompletionTemplateWithSort             -- KW["completion"] KW["template"] KW[":"] _1 KW["="] _2 _3 _4,
   CompletionTemplateWithSort.3:iter-star -- _1,
   Disable                                -- KW["(disable)"],
   Disable                                -- KW["(disable)"],
   PPTable                                -- KW["pp-table"] KW[":"] _1,
   ReferenceHoverRule                     -- KW["reference"] _1 KW[":"] _2 _3,
   PrettyPrint                            -- KW["pretty-print"] KW[":"] _1,
   TextReconstruction                     -- KW["text"] KW["reconstruction"] KW[":"] _1,
   Refactoring                            -- KW["refactoring"] _1 KW[":"] _2 KW["="] _3 _4 _5,
   Refactoring.1:iter-star                -- _1,
   Refactoring.4:iter-star                -- _1,
   Refactoring.5:iter-star                -- _1,
   Shortcut                               -- KW["shortcut"] KW[":"] _1,
   Shortcut                               -- KW["shortcut"] KW[":"] _1,
   InteractionId                          -- _1,
   UserInput                              -- V  [H  [KW["input"]] _1],
   UserInput.1:iter                       -- _1,
   IdInputField                           -- KW["identifier"] KW[":"] _1 KW["="] _2,
   TextInputField                         -- KW["text"] KW[":"] _1 KW["="] _2,
   BooleanInputField                      -- KW["boolean"] KW[":"] _1 KW["="] _2,
   TrueValue                              -- KW["true"],
   FalseValue                             -- KW["false"],
   KeyBinding                             -- _1 KW["="] _2,
   KeyCombination                         -- _1,
   KeyCombination.1:iter-sep              -- _1 KW["+"],
   SemanticObserverDeprecated             -- KW["observer"] KW[":"] _1 _2,
   SemanticObserverDeprecated.2:iter-star -- _1,
   SemanticProviderDeprecated             -- KW["provider"] KW[":"] _1,
   OnSaveDeprecated                       -- KW["on"] KW["save"] KW[":"] _1 _2,
   Builder                                -- KW["builder"] KW[":"] _1 KW["="] _2 _3,
   Builder.3:iter-star                    -- _1,
   BuilderCaption                         -- KW["builder"] KW["caption"] KW[":"] _1,
   OpenEditor                             -- KW["(openeditor)"],
   RealTime                               -- KW["(realtime)"],
   Persistent                             -- KW["(persistent)"],
   Meta                                   -- KW["(meta)"],
   Cursor                                 -- KW["(cursor)"],
   Source                                 -- KW["(source)"],
   CompletionTrigger                      -- KW["completion"] KW["trigger"] KW[":"] _1 _2,
   CompletionProposer                     -- KW["completion"] KW["proposer"] _1 KW[":"] _2,
   CompletionProposer.1:iter-star         -- _1,
   CompletionKeyword                      -- KW["completion"] KW["keyword"] KW[":"] _1 _2,
   CompletionTemplateEx                   -- KW["completion"] KW["template"] _1 KW[":"] _2 _3 _4,
   CompletionTemplateEx.1:iter-star       -- _1,
   CompletionTemplateEx.3:iter            -- _1,
   CompletionTemplateEx.4:iter-star       -- _1,
   NoCompletionPrefix                     -- ,
   CompletionPrefix                       -- _1 KW["="],
   Placeholder                            -- _1,
   Cursor                                 -- KW["(cursor)"],
   PlaceholderWithSort                    -- _1 _2 KW[">"],
   None                                   -- ,
   Disable                                -- KW["(disabled)"],
   Blank                                  -- KW["(blank)"],
   Linked                                 -- KW["(linked)"],
   ReferenceRule                          -- KW["reference"] _1 KW[":"] _2,
   HoverRule                              -- KW["hover"] _1 KW[":"] _2,
   OccurrenceRule                         -- KW["occurrence"] _1 KW[":"] _2,
   ColorRuleAll                           -- KW["environment"] _1 KW[":"] _2,
   ColorRule                              -- _1 KW[":"] _2,
   ColorRuleAllNamed                      -- KW["environment"] _1 KW[":"] _2 KW["="] _3,
   ColorRuleNamed                         -- _1 KW[":"] _2 KW["="] _3,
   Attribute                              -- _1 _2 _3,
   AttributeRef                           -- _1,
   Token                                  -- _1,
   Literal                                -- KW["token"] _1,
   TK_IDENTIFIER                          -- KW["identifier"],
   TK_NUMBER                              -- KW["number"],
   TK_LAYOUT                              -- KW["layout"],
   TK_STRING                              -- KW["string"],
   TK_KEYWORD                             -- KW["keyword"],
   TK_OPERATOR                            -- KW["operator"],
   TK_VAR                                 -- KW["var"],
   TK_ERROR                               -- KW["error"],
   TK_UNKNOWN                             -- KW["unknown"],
   NORMAL                                 -- ,
   BOLD                                   -- KW["bold"],
   ITALIC                                 -- KW["italic"],
   BOLD_ITALIC                            -- KW["bold"] KW["italic"],
   BOLD_ITALIC                            -- KW["italic"] KW["bold"],
   ColorDefault                           -- KW["_"],
   ColorRGB                               -- _1 _2 _3,
   NoColor                                -- ,
   ColorDef                               -- _1 KW["="] _2,
   OutlineRule                            -- _1,
   FoldRule                               -- _1 _2,
   FoldRuleAll                            -- KW["all"] _1 _2,
   Disable                                -- KW["(disabled)"],
   Folded                                 -- KW["(folded)"],
   None                                   -- ,
   Strategy                               -- _1,
   Attribute                              -- KW["id"] KW["."] _1,
   None                                   -- ,
   Values                                 -- _1,
   Values.1:iter-star-sep                 -- _1 KW[","],
   LanguageName                           -- KW["name"] KW[":"] _1,
   LanguageId                             -- KW["id"] KW[":"] _1,
   Extensions                             -- KW["extensions"] KW[":"] _1,
   Description                            -- KW["description"] KW[":"] _1,
   Table                                  -- KW["table"] KW[":"] _1,
   TableProvider                          -- KW["table"] KW["provider"] KW[":"] _1,
   StartSymbols                           -- V  [H  [KW["start"] KW["symbols"] KW[":"]] _1],
   StartSymbols.1:iter-star               -- _1,
   StartSymbols                           -- KW["start"] KW["symbols"] KW[":"] _1,
   URL                                    -- KW["url"] KW[":"] _1,
   Extends                                -- KW["extends"] KW[":"] _1,
   Aliases                                -- KW["aliases"] KW[":"] _1,
   UnmanagedTablePrefix                   -- KW["unmanaged"] KW["table"] KW[":"] _1 KW["*"],
   Disambiguator                          -- KW["disambiguator"] KW[":"] _1,
   FlattenUnicode                         -- KW["unicode"] KW["flatten"] KW[":"] _1,
   SemanticObserver                       -- KW["observer"] KW[":"] _1 _2,
   SemanticObserver.2:iter-star           -- _1,
   SemanticProvider                       -- KW["provider"] KW[":"] _1,
   MultiFile                              -- KW["("] KW["multifile"] KW[")"],
   OnSave                                 -- KW["on"] KW["save"] KW[":"] _1 _2,
   LineCommentPrefix                      -- KW["line"] KW["comment"] KW[":"] _1,
   BlockCommentDefs                       -- KW["block"] KW["comment"] KW[":"] _1,
   FenceDefs                              -- V  [H  [KW["fences"] KW[":"]] _1],
   FenceDefs.1:iter-star                  -- _1,
   IndentDefs                             -- V  [H  [KW["indent"] KW["after"] KW[":"]] _1],
   IndentDefs.1:iter-star                 -- _1,
   IdentifierLexical                      -- KW["identifier"] KW["lexical"] KW[":"] _1,
   BlockCommentDef                        -- _1 _2 _3,
   BlockCommentDef                        -- _1 _2 _3,
   NoContinuation                         -- ,
   FenceDef                               -- _1 _2,
   IndentDef                              -- _1,
   True                                   -- KW["true"],
   False                                  -- KW["false"],
   Sort                                   -- _1,
   ListSort                               -- _1 KW["*"],
   String                                 -- _1,
   ConstructorOnly                        -- KW["_"] KW["."] _1,
   Constructor                            -- _1,
   SortAndConstructor                     -- _1 KW["."] _2,
   Module                                 -- KW["module"] _1 _2 _3,
   Module.3:iter-star                     -- _1,
   Imports                                -- V  [H  [KW["imports"]] _1],
   Imports.1:iter                         -- _1,
   NoImports                              -- ,
   Import                                 -- _1,
   ImportRenamed                          -- _1 KW["["] _2 KW["]"]
]