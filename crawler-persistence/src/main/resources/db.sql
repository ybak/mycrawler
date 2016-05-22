CREATE TABLE `cdgh` (
	`id` INT(11) NOT NULL AUTO_INCREMENT,
	`url` VARCHAR(128) NOT NULL,
	`html` TEXT NULL,
	PRIMARY KEY (`id`)
)
COLLATE='utf8_general_ci'
ENGINE=InnoDB
AUTO_INCREMENT=0;

CREATE TABLE `cdgh_extract` (
	`id` INT(11) NOT NULL,
	`url` VARCHAR(128) NOT NULL,
	`title` VARCHAR(255) NULL DEFAULT NULL,
	`publish_time` DATETIME NULL DEFAULT NULL,
	PRIMARY KEY (`id`),
	CONSTRAINT `FK_cdgh_extract_cdgh` FOREIGN KEY (`id`) REFERENCES `cdgh` (`id`)
)
COLLATE='utf8_general_ci'
ENGINE=InnoDB
ROW_FORMAT=COMPACT;


CREATE TABLE `chengdu12345` (
	`id` int(11) NOT NULL AUTO_INCREMENT,
	`url` varchar(128) NOT NULL,
	`title` varchar(256) NOT NULL DEFAULT '',
	`sender` varchar(50) DEFAULT NULL,
	`accept_unit` varchar(50) DEFAULT NULL,
	`status` varchar(50) DEFAULT NULL,
	`category` varchar(50) DEFAULT NULL,
	`views` int(11) DEFAULT NULL,
	`create_date` datetime DEFAULT NULL,
	`content` text,
	`result` text,
	PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;