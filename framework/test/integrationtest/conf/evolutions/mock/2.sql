#
# Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
#
# Semicolon-escaping test. If we weren't escaping ";;" correctly then
# executing this up would fail.

# --- !Ups

CREATE OR REPLACE VIEW SemicolonEscapeTest AS SELECT * FROM Mock WHERE value = ';;';

# --- !Downs

DROP VIEW SemicolonEscapeTest;