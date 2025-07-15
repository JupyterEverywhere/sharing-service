--
-- Populate readable IDs table with adjective-adjective-animal combinations
--

-- Step 1: Create procedure to generate readable IDs
CREATE OR REPLACE PROCEDURE populate_readable_ids()
    LANGUAGE plpgsql AS $$

DECLARE
    adjectives_list VARCHAR(32)[] := ARRAY [
        'adaptable', 'adorable', 'adored', 'adventurous', 'affable', 'affectionate', 'agreeable',
        'amazing', 'ambitious', 'amiable', 'amicable', 'amusing', 'angelic', 'appreciated', 'appreciative', 'authentic',
        'awesome', 'balanced', 'beautiful', 'beloved', 'beyond-fabulous', 'blessed', 'blissful', 'blithesome', 'bold',
        'brave', 'breathtaking', 'bright', 'brilliant', 'broad-minded', 'calm', 'capable', 'careful', 'caring',
        'centered', 'charismatic', 'charming', 'cheerful', 'cherished', 'comfortable', 'communicative', 'compassionate',
        'confident', 'conscientious', 'considerate', 'content', 'convivial', 'courageous', 'courteous', 'creative',
        'cute', 'daring', 'dazzling', 'decisive', 'dedicated', 'delightful', 'desirable', 'determined', 'diligent',
        'divine', 'easygoing', 'empowered', 'enchanted', 'energetic', 'energized', 'enlightened', 'enthusiastic',
        'excellent', 'excited', 'exhilarated', 'expansive', 'exquisite', 'extraordinary', 'exuberant', 'fabulous',
        'fair-minded', 'faithful', 'fantastic', 'favorable', 'fearless', 'focused', 'forgiving', 'fortuitous',
        'free-spirited', 'friendly', 'fulfilled', 'fun-loving', 'funny', 'generous', 'genial', 'genius', 'gentle',
        'genuine', 'giving', 'glad', 'glorious', 'glowing', 'good', 'healthy', 'graceful', 'gracious', 'grateful',
        'great', 'gregarious', 'grounded', 'happy-hearted', 'hard-working', 'harmonious', 'heartfull', 'heartwarming',
        'heavenly', 'helpful', 'high-spirited', 'honest', 'hopeful', 'imaginative', 'incomparable', 'incredible',
        'independent', 'ineffable', 'innovative', 'inspirational', 'inspired', 'intellectual', 'intelligent',
        'intuitive', 'inventive', 'invigorated', 'involved', 'irresistible', 'jolly', 'jovial', 'joyful', 'joyous',
        'jubilant', 'kind', 'kind-hearted', 'kissable', 'knowledgeable', 'lively', 'lovable', 'lovely', 'loving',
        'loyal', 'lucky', 'magical', 'magnificent', 'marvelous', 'memorable', 'mind-blowing', 'mindful', 'miraculous',
        'mirthful', 'modest', 'nice', 'noble', 'nurtured', 'open-hearted', 'open-minded', 'optimistic', 'original',
        'outstanding', 'passionate', 'patient', 'peaceful', 'perfect', 'persistent', 'pioneering', 'placid', 'playful',
        'polite', 'positive', 'powerful', 'precious', 'prosperous', 'quick-witted', 'radient', 'rational', 'relaxed',
        'reliable', 'remarkable', 'reserved', 'resilient', 'resourceful', 'satisfied', 'self-confident', 'self-disciplined',
        'sensational', 'sensible', 'sensitive', 'serene', 'shining', 'shy', 'sincere', 'smart', 'soulful', 'spectacular',
        'splendid', 'stellar', 'strong', 'stupendous', 'successful', 'super', 'thoughtful', 'thrilled', 'thriving',
        'tough', 'tranquil', 'triumphant', 'unassuming', 'unbelievable', 'understanding', 'unique', 'uplifted', 'versatile',
        'vibrant', 'victorious', 'vivacious', 'warm', 'warmhearted', 'wholehearted', 'wise', 'witty', 'wonderful',
        'wondrous', 'worthy', 'young-at-heart', 'youthful', 'zappy', 'zestful'
        ];
    animals_list VARCHAR(32)[] := ARRAY [
        'aardvark', 'albatross', 'alligator', 'alpaca', 'ant', 'anteater', 'antelope', 'armadillo', 'badger', 'barracuda',
        'bat', 'bear', 'beaver', 'bee', 'bison', 'buffalo', 'butterfly', 'camel', 'capybara', 'caribou', 'cassowary',
        'cat', 'caterpillar', 'cheetah', 'chicken', 'chinchilla', 'clam', 'cobra', 'cod', 'coyote', 'crab', 'crane',
        'crocodile', 'crow', 'deer', 'dogfish', 'dolphin', 'dotterel', 'dove', 'dragonfly', 'duck', 'eagle', 'echidna',
        'eel', 'elephant', 'elk', 'emu', 'falcon', 'ferret', 'finch', 'fish', 'flamingo', 'fly', 'fox', 'frog', 'gazelle',
        'gerbil', 'giraffe', 'goat', 'goldfish', 'goose', 'grasshopper', 'hamster', 'hare', 'hawk', 'hedgehog', 'heron',
        'herring', 'human', 'hummingbird', 'hyena', 'ibex', 'jackal', 'jaguar', 'jellyfish', 'kangaroo', 'kingfisher',
        'koala', 'kookabura', 'lark', 'lemur', 'leopard', 'lion', 'llama', 'lobster', 'magpie', 'mallard', 'mantis',
        'meerkat', 'mink', 'mole', 'mongoose', 'moose', 'mouse', 'mule', 'narwhal', 'newt', 'nightingale', 'octopus',
        'opossum', 'ostrich', 'otter', 'owl', 'oyster', 'panther', 'parrot', 'partridge', 'peafowl', 'pelican', 'penguin',
        'pheasant', 'pigeon', 'pony', 'porcupine', 'porpoise', 'quail', 'rabbit', 'raccoon', 'ram', 'rat', 'raven',
        'red panda', 'reindeer', 'rhinoceros', 'salamander', 'salmon', 'sand dollar', 'sandpiper', 'sardine', 'scorpion',
        'seahorse', 'seal', 'shark', 'sheep', 'skunk', 'snail', 'snake', 'sparrow', 'spider', 'spoonbill', 'squid',
        'squirrel', 'starling', 'stingray', 'stork', 'swallow', 'swan', 'termite', 'tiger', 'toad', 'trout', 'turtle',
        'unicorn', 'viper', 'wallaby', 'walrus', 'wasp', 'weasel', 'wildcat', 'wolf', 'wolverine', 'wombat', 'woodpecker',
        'worm', 'yak', 'zebra'
        ];
BEGIN

    -- Temporarily disable primary key constraint to speed up insert, distinct keyword in insert ensures uniqueness
    ALTER TABLE jupyter_notebooks_metadata_readable_ids DROP CONSTRAINT IF EXISTS jupyter_notebooks_metadata_readable_ids_pkey;

    -- Insert into readable_ids table
    INSERT INTO jupyter_notebooks_metadata_readable_ids(readable_id)
    SELECT DISTINCT adj1.word || '-' || adj2.word || '-' || an.word AS readable_id
    FROM unnest(animals_list) AS an(word)
             JOIN LATERAL unnest(adjectives_list) AS adj1(word) ON TRUE
             JOIN LATERAL unnest(adjectives_list) adj2(word) ON adj1.word <> adj2.word
    ON CONFLICT DO NOTHING;

    -- Reinstate primary key constraint
    ALTER TABLE jupyter_notebooks_metadata_readable_ids ADD CONSTRAINT jupyter_notebooks_metadata_readable_ids_pkey PRIMARY KEY (readable_id);

END $$;

-- Step 2: Call procedure to populate the table
CALL populate_readable_ids();

-- Step 3: Clean up by dropping the procedure
DROP PROCEDURE IF EXISTS populate_readable_ids;
