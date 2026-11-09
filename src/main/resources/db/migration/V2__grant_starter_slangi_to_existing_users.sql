INSERT INTO user_malangis (user_id, malangi_type, is_unlocked, is_selected)
SELECT users.id, 'SLANGI', TRUE,
       NOT EXISTS (
           SELECT 1
           FROM user_malangis AS selected_malangi
           WHERE selected_malangi.user_id = users.id
             AND selected_malangi.is_selected = TRUE
       )
FROM users
WHERE NOT EXISTS (
    SELECT 1
    FROM user_malangis
    WHERE user_malangis.user_id = users.id
      AND user_malangis.malangi_type = 'SLANGI'
);
